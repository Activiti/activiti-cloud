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

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

@AutoConfiguration(before = BinderFactoryAutoConfiguration.class,
                   after = {InputBindingConfiguration.class,OutputBindingConfiguration.class})
@ConditionalOnClass(BindingServiceProperties.class)
public class FunctionBindingConfiguration extends AbstractFunctionalBindingConfiguration {

    @Bean
    public BindingResolver bindingResolver(BindingServiceProperties bindingServiceProperties){
       return destination ->  Optional
            .ofNullable(bindingServiceProperties.getBindingDestination(destination))
            .orElse(destination);
    }

    @Bean
    public FunctionBindingPropertySource functionDefinitionPropertySource(ConfigurableApplicationContext applicationContext) {
        return new FunctionBindingPropertySource(applicationContext.getEnvironment());
    }

    @Bean
    public FunctionAnnotationService functionAnnotationService(DefaultListableBeanFactory beanFactory) {
        return new FunctionAnnotationService(beanFactory);
    }

    @Bean(name = "functionBindingBeanPostProcessor")
    public BeanPostProcessor functionBindingBeanPostProcessor(FunctionAnnotationService functionAnnotationService,
                                                              IntegrationFlowContext integrationFlowContext,
                                                              Function<String, String> resolveExpression) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Supplier.class.isInstance(bean) ||
                    Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)) {

                    Optional.ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, FunctionBinding.class))
                        .ifPresent(functionBinding -> {

                            Type functionType = discoverFunctionType(bean, beanName);

                            FunctionRegistration functionRegistration = new FunctionRegistration(bean).type(functionType);

                            registerFunctionRegistration(beanName,
                                                         functionRegistration);

                            GenericSelector<Message<?>> selector = Optional.ofNullable(functionBinding)
                                                                           .map(FunctionBinding::condition)
                                                                           .filter(StringUtils::hasText)
                                                                           .map(resolveExpression)
                                                                           .map(ExpressionEvaluatingSelector::new)
                                                                           .orElseGet(() -> new ExpressionEvaluatingSelector("true"));

                            if (Supplier.class.isInstance(bean)) {
                                FunctionInvocationWrapper supplier = functionFromDefinition(beanName);

                                IntegrationFlowBuilder supplierFlowBuilder = IntegrationFlows.fromSupplier(supplier)
                                                                                             .filter(selector,  filter -> filter.discardChannel("nullChannel")
                                                                                                                                .throwExceptionOnRejection(true))
                                                                                             .log(LoggingHandler.Level.INFO, beanName + "." + functionBinding.output())
                                                                                             .channel(functionBinding.output());
                                integrationFlowContext.registration(supplierFlowBuilder.get())
                                                      .register();
                           }
                            else {
                                GenericHandler<Message> handler = (message, headers) -> {
                                    FunctionInvocationWrapper function = functionFromDefinition(beanName);
                                    return function.apply(message);
                                };

                                IntegrationFlowBuilder functionFlowBuilder = IntegrationFlows.from(getGatewayInterface(Function.class.isInstance(bean)),
                                                                                                   gateway -> gateway.replyTimeout(0L)
                                                                                                                     .errorChannel("errorChannel"))
                                                                                             .log(LoggingHandler.Level.INFO, beanName + "." + functionBinding.input())
                                                                                             .filter(selector)
                                                                                             .handle(Message.class, handler);
                                if (Function.class.isInstance(bean)) {
                                    functionFlowBuilder.bridge()
                                                       .log(LoggingHandler.Level.INFO, beanName + "." + functionBinding.output())
                                                       .channel(functionBinding.output());
                                }

                                IntegrationFlow inputChannelFlow = IntegrationFlows.from(functionBinding.input())
                                                                                   .gateway(functionFlowBuilder.get(),
                                                                                            spec -> spec.replyTimeout(0L)
                                                                                                        .errorChannel("errorChannel"))
                                                                                   .get();

                                integrationFlowContext.registration(inputChannelFlow)
                                                      .register();
                            }
                        });
                }

                return bean;
            }
        };
    }

    public interface BindingResolver extends Function<String, String> {}
}
