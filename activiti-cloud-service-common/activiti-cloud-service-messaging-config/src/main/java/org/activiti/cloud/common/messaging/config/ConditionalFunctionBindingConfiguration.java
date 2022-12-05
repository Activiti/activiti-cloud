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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionBinding;
import org.activiti.cloud.common.messaging.functional.ConnectorGateway;
import org.activiti.cloud.common.messaging.functional.ConsumerGateway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass(FunctionBindingConfiguration.class)
@AutoConfigureAfter(FunctionBindingConfiguration.class)
public class ConditionalFunctionBindingConfiguration extends AbstractFunctionalBindingConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public MessageRoutingCallback defaultMessageRoutingCallback() {
        return null;
    }

    @Bean(name = "conditionalFunctionBindingPostProcessor")
    public BeanPostProcessor conditionalFunctionBindingPostProcessor(DefaultListableBeanFactory beanFactory,
        IntegrationFlowContext integrationFlowContext,
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties,
        FunctionAnnotationService functionAnnotationService,
        FunctionBindingPropertySource functionBindingPropertySource,
        Function<String, String> resolveExpression,
        ConfigurableEnvironment environment) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)) {

                    Optional.ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, ConditionalFunctionBinding.class))
                        .ifPresent(functionDefinition -> {
                            final String gatewayName = beanName + "Gateway";

                            functionBindingPropertySource.register(beanName);
                            functionBindingPropertySource.register(gatewayName);

                            final boolean hasOutput = StringUtils.hasText(functionDefinition.output());

                            final String gatewayInName = getInBinding(gatewayName);
                            final String gatewayOutName = getOutBinding(gatewayName);

                            setOutput(gatewayOutName, functionDefinition.output(), bindingServiceProperties, streamFunctionProperties, environment);
                            setInput(gatewayInName, functionDefinition.input(), streamFunctionProperties, bindingServiceProperties);

                            GenericSelector<Message<?>> selector = Optional.ofNullable(
                                    beanFactory.findAnnotationOnBean(beanName, ConditionalFunctionBinding.class))
                                .map(ConditionalFunctionBinding::condition)
                                .filter(StringUtils::hasText)
                                .map(resolveExpression)
                                .map(ExpressionEvaluatingSelector::new)
                                .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                            MimeType bindingContentTypeTransformer = getContentTypeTransformer(bindingServiceProperties, functionDefinition.input());

                            IntegrationFlowBuilder connectorFlowBuilder = createFlowBuilder(gatewayName, hasOutput)
                                .log(Level.INFO, gatewayName + ".request")
                                .filter(selector)
                                .handle(Message.class, createHandler(beanName));

                            if (hasOutput) {
                                connectorFlowBuilder.log(LoggingHandler.Level.INFO, gatewayName + ".result")
                                    .bridge();
                            }

                            integrationFlowContext.registration(connectorFlowBuilder.get())
                                .register();
                        });
                }
                return bean;
            }
        };
    }

    protected MimeType getContentTypeTransformer(BindingServiceProperties bindingServiceProperties, String binding){
        return Optional.ofNullable(bindingServiceProperties.getBindingProperties(binding))
            .filter(Objects::nonNull)
            .map(BindingProperties::getContentType)
            .map(MimeType::valueOf)
            .orElse(null);
    }

    protected IntegrationFlowBuilder createFlowBuilder(String functionName, boolean hasOutput){
        if(hasOutput){
            return IntegrationFlows.from(ConnectorGateway.class, (gateway) -> gateway.beanName(functionName)
                .replyTimeout(0L));
        } else {
            return IntegrationFlows.from(ConsumerGateway.class, (gateway) -> gateway.beanName(functionName)
                .replyTimeout(0L));
        }
    }

    protected GenericHandler<Message> createHandler(String beanName) {
        return (message, headers) -> {
            FunctionInvocationWrapper function = this.functionFromDefinition(beanName);
            return function.apply(message);
        };
    }


}
