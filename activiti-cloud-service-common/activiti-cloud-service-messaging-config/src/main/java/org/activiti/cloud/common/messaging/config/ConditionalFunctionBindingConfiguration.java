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

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.ConditionalFunctionBinding;
import org.activiti.cloud.common.messaging.functional.ConditionalMessageRoutingCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.util.StringUtils;

@Configuration
@AutoConfigureBefore(BinderFactoryAutoConfiguration.class)
@ConditionalOnClass(BindingServiceProperties.class)
public class ConditionalFunctionBindingConfiguration {

    @Autowired
    private FunctionCatalog functionCatalog;

    @Bean
    public BeanPostProcessor conditionalFunctionBindingBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        FunctionBindingPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Consumer.class.isInstance(bean) ||
                    Function.class.isInstance(bean)) {

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConditionalFunctionBinding.class))
                        .ifPresent(functionDefinition -> {
                            String listenerName = beanName;

                            functionDefinitionPropertySource.register(listenerName);

                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    Optional.ofNullable(bindingServiceProperties.getBindingDestination(output))
                                        .ifPresentOrElse(
                                            binding -> streamFunctionProperties.getBindings()
                                                .put(beanName + "-out-0", binding),
                                            () -> streamFunctionProperties.getBindings()
                                                .put(beanName + "-out-0", output)
                                        );
                                });

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    String routerName = input + "Router";
                                    String routerCallbackName = routerName + "Callback";

                                    Optional.of(functionDefinition.condition())
                                        .filter(StringUtils::hasText)
                                        .ifPresentOrElse(condition -> {
                                                try {
                                                    RoutingFunction router = beanFactory.getBean(routerName, RoutingFunction.class);
                                                    try {
                                                        ConditionalMessageRoutingCallback callback = beanFactory.getBean(routerCallbackName,
                                                            ConditionalMessageRoutingCallback.class);
                                                        callback.addRoutingExpression(beanName, condition);
                                                    } catch (BeansException e) {
                                                        throw new IllegalStateException(
                                                            String.format("Router %s is defined, but it has no proper callback", routerName));
                                                    }
                                                } catch (BeansException e) {
                                                    ConditionalMessageRoutingCallback callback = createCallback(beanFactory, routerCallbackName);
                                                    callback.addRoutingExpression(beanName, condition);

                                                    RoutingFunction router = createRouter(beanFactory, routerName, callback);
                                                    functionDefinitionPropertySource.register(routerName);
                                                    streamFunctionProperties.getBindings().put(routerName + "-in-0", input);
                                                }
                                            },
                                            () -> streamFunctionProperties.getBindings()
                                                .put(listenerName + "-in-0", input)
                                        );
                                });
                        });
                }

                return bean;
            }
        };
    }

    protected ConditionalMessageRoutingCallback createCallback(DefaultListableBeanFactory beanFactory, String routerCallbackName) {
        ConditionalMessageRoutingCallback callback = new ConditionalMessageRoutingCallback();
        beanFactory.registerSingleton(routerCallbackName, callback);
        return callback;
    }

    protected RoutingFunction createRouter(DefaultListableBeanFactory beanFactory, String routerName, ConditionalMessageRoutingCallback callback) {
        RoutingFunction routingFunction = new RoutingFunction(functionCatalog, Collections.emptyMap(), new BeanFactoryResolver(beanFactory), callback);
        beanFactory.registerSingleton(routerName, routingFunction);
        return routingFunction;
    }


}
