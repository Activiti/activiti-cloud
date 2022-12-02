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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass(FunctionBindingConfiguration.class)
@AutoConfigureAfter(FunctionBindingConfiguration.class)
public class ConditionalFunctionBindingConfiguration extends AbstractFunctionalBindingConfiguration implements ApplicationContextAware {

    private static final String ROUTER_BEAN_NAME_PATTERN = "%sRouter";
    private static final String ROUTER_CALLBACK_BEAN_NAME_PATTERN = "%sCallback";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.notNull(applicationContext,
            "ConditionalFunctionBindingConfiguration can not process beans because the application context is null");
        this.applicationContext = applicationContext;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public MessageRoutingCallback defaultMessageRoutingCallback(){
        return null;
    }

    @Bean
    public BeanPostProcessor conditionalFunctionBindingBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        FunctionBindingPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties,
        FunctionAnnotationService functionAnnotationService,
        ConfigurableEnvironment environment) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (Consumer.class.isInstance(bean) ||
                    Function.class.isInstance(bean)) {

                    Optional.ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, ConditionalFunctionBinding.class))
                        .ifPresent(functionDefinition -> {
                            String listenerName = beanName;

                            final String beanOutName = getOutBinding(beanName);

                            functionDefinitionPropertySource.register(listenerName);

                            setOutput(beanOutName, functionDefinition.output(), bindingServiceProperties, streamFunctionProperties, environment);

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    String routerName = String.format(ROUTER_BEAN_NAME_PATTERN, input);
                                    String routerCallbackName = String.format(ROUTER_CALLBACK_BEAN_NAME_PATTERN, routerName);

                                    Optional.of(functionDefinition.condition())
                                        .filter(StringUtils::hasText)
                                        .ifPresentOrElse(condition -> {
                                                try {
                                                    beanFactory.getBean(routerName, RoutingFunction.class);
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

                                                    createRouter(beanFactory, routerName, callback);
                                                    functionDefinitionPropertySource.register(routerName);
                                                    streamFunctionProperties.getBindings().put(
                                                        getInBinding(routerName), input);
                                                }
                                            },
                                            () -> streamFunctionProperties.getBindings()
                                                .put(getInBinding(listenerName), input)
                                        );
                                });
                        });
                }

                return bean;
            }
        };
    }

    private FunctionCatalog getFunctionCatalog() {
        return applicationContext.getBean(FunctionCatalog.class);
    }

    protected ConditionalMessageRoutingCallback createCallback(DefaultListableBeanFactory beanFactory, String routerCallbackName) {
        ConditionalMessageRoutingCallback callback = new ConditionalMessageRoutingCallback();
        beanFactory.registerBeanDefinition(routerCallbackName,
            new RootBeanDefinition(ConditionalMessageRoutingCallback.class, BeanDefinition.SCOPE_SINGLETON, () -> callback));
        return callback;
    }

    protected RoutingFunction createRouter(DefaultListableBeanFactory beanFactory, String routerName, ConditionalMessageRoutingCallback callback) {
        RoutingFunction routingFunction = new RoutingFunction(getFunctionCatalog(), Collections.emptyMap(), new BeanFactoryResolver(beanFactory), callback);
        beanFactory.registerBeanDefinition(routerName,
            new RootBeanDefinition(RoutingFunction.class, BeanDefinition.SCOPE_SINGLETON, () -> routingFunction));
        return routingFunction;
    }

}
