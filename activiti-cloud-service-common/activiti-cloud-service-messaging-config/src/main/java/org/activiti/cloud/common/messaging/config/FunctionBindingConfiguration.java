/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.common.messaging.config;

import static org.springframework.integration.handler.LoggingHandler.Level.DEBUG;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.function.json.JacksonMapper;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

@AutoConfiguration(after = BinderFactoryAutoConfiguration.class, before = FunctionConfiguration.class)
@ConditionalOnClass(BindingServiceProperties.class)
public class FunctionBindingConfiguration extends AbstractFunctionalBindingConfiguration {

    public static final String FUNCTION_BINDING_SELECTOR_DISCARD_FLOW = "functionBindingSelectorDiscardFlow";
    public static final String FUNCTION_BINDING_SELECTOR_DISCARD_CHANNEL = "functionBindingSelectorDiscardChannel";
    public static final String NULL_CHANNEL = "nullChannel";

    @Bean
    public BindingResolver bindingResolver(
        BindingServiceProperties bindingServiceProperties,
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return bindingName ->
            Optional
                .of(messagingProperties.getFunctionRouter())
                .filter(ActivitiCloudMessagingProperties.FunctionRouterProperties::isEnabled)
                .map(functionRouter -> functionRouter.destinations().get(bindingName))
                .or(() ->
                    Optional
                        .ofNullable(bindingServiceProperties.getBindings().get(bindingName))
                        .map(BindingProperties::getDestination)
                )
                .orElse(bindingName);
    }

    @Bean
    public FunctionBindingPropertySource functionDefinitionPropertySource(
        ConfigurableApplicationContext applicationContext
    ) {
        return new FunctionBindingPropertySource(applicationContext.getEnvironment());
    }

    @Bean
    public FunctionAnnotationService functionAnnotationService(DefaultListableBeanFactory beanFactory) {
        return new FunctionAnnotationService(beanFactory);
    }

    @Bean("resolveExpression")
    public Function<String, String> resolveExpression(ConfigurableApplicationContext applicationContext) {
        return value -> {
            BeanExpressionResolver resolver = applicationContext.getBeanFactory().getBeanExpressionResolver();
            BeanExpressionContext expressionContext = new BeanExpressionContext(
                applicationContext.getBeanFactory(),
                null
            );

            String resolvedValue = applicationContext.getBeanFactory().resolveEmbeddedValue(value);
            if (resolvedValue.startsWith("#{") && value.endsWith("}")) {
                resolvedValue = (String) resolver.evaluate(resolvedValue, expressionContext);
            }
            return resolvedValue;
        };
    }

    @Bean(name = FUNCTION_BINDING_SELECTOR_DISCARD_FLOW)
    IntegrationFlow functionBindingSelectorDiscardFlow() {
        return IntegrationFlow
            .from(FUNCTION_BINDING_SELECTOR_DISCARD_CHANNEL)
            .log(DEBUG, FUNCTION_BINDING_SELECTOR_DISCARD_FLOW)
            .channel(NULL_CHANNEL)
            .get();
    }

    @Bean(name = "functionBindingBeanPostProcessor")
    public BeanPostProcessor functionBindingBeanPostProcessor(
        FunctionAnnotationService functionAnnotationService,
        IntegrationFlowContext integrationFlowContext,
        Function<String, String> resolveExpression,
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (
                    Supplier.class.isInstance(bean) ||
                    Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)
                ) {
                    Optional
                        .ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, FunctionBinding.class))
                        .ifPresent(functionBinding -> {
                            final Type functionType = discoverFunctionType(bean, beanName);
                            final var functionRouter = messagingProperties.getFunctionRouter();

                            FunctionRegistration functionRegistration = new FunctionRegistration(bean)
                                .type(functionType);

                            var functionBeanName = registerFunctionRegistration(beanName, functionRegistration);

                            if (functionRouter.isEnabled()) {
                                functionRouter.register(functionBinding.input(), functionBeanName);
                            }

                            GenericSelector<Message<?>> selector = Optional
                                .ofNullable(functionBinding)
                                .map(FunctionBinding::condition)
                                .filter(StringUtils::hasText)
                                .map(resolveExpression)
                                .map(ExpressionEvaluatingSelector::new)
                                .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                            if (Supplier.class.isInstance(bean)) {
                                FunctionInvocationWrapper supplier = functionFromDefinition(beanName);

                                IntegrationFlowBuilder supplierFlowBuilder = IntegrationFlow
                                    .fromSupplier(supplier)
                                    .filter(
                                        selector,
                                        filter ->
                                            filter
                                                .discardChannel(FUNCTION_BINDING_SELECTOR_DISCARD_CHANNEL)
                                                .throwExceptionOnRejection(false)
                                    )
                                    .log(DEBUG, beanName + "." + functionBinding.output())
                                    .channel(functionBinding.output());
                                integrationFlowContext.registration(supplierFlowBuilder.get()).register();
                            } else {
                                GenericHandler<Message> handler = (message, headers) -> {
                                    FunctionInvocationWrapper function = functionFromDefinition(beanName);
                                    return function.apply(message);
                                };

                                IntegrationFlowBuilder functionFlowBuilder = IntegrationFlow
                                    .from(
                                        getGatewayInterface(Function.class.isInstance(bean)),
                                        gateway -> gateway.replyTimeout(0L)
                                    )
                                    .log(DEBUG, beanName + "." + functionBinding.input())
                                    .filter(
                                        selector,
                                        filter ->
                                            filter
                                                .discardChannel(FUNCTION_BINDING_SELECTOR_DISCARD_CHANNEL)
                                                .throwExceptionOnRejection(false)
                                    )
                                    .handle(Message.class, handler);
                                if (Function.class.isInstance(bean)) {
                                    functionFlowBuilder
                                        .bridge()
                                        .log(DEBUG, beanName + "." + functionBinding.output())
                                        .channel(functionBinding.output());
                                }

                                IntegrationFlow inputChannelFlow = IntegrationFlow
                                    .from(functionBinding.input())
                                    .gateway(functionFlowBuilder.get(), spec -> spec.replyTimeout(0L))
                                    .get();

                                integrationFlowContext.registration(inputChannelFlow).register();
                            }
                        });
                }

                return bean;
            }
        };
    }

    public interface BindingResolver extends Function<String, String> {
        default String getBindingDestination(String bindingName) {
            return apply(bindingName);
        }
    }

    @Bean
    @ConditionalOnClass(JacksonMapper.class)
    @Primary
    public JacksonMapper jacksonMapper(@Autowired(required = false) ObjectMapper objectMapper) {
        //temporary workaround for https://github.com/spring-cloud/spring-cloud-function/issues/1159
        ObjectMapper copiedMapper;
        if (objectMapper == null) {
            copiedMapper = new ObjectMapper();
            copiedMapper.registerModule(new JavaTimeModule());
            copiedMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            copiedMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
            copiedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } else {
            try {
                copiedMapper = objectMapper.copy();
            } catch (Exception e) {
                copiedMapper = new ObjectMapper();
            }
        }
        //logic from AlfrescoWebAutoConfiguration.configureObjectMapperForBigDecimal
        copiedMapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        return new JacksonMapper(copiedMapper);
    }
}
