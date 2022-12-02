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
    public MessageRoutingCallback defaultMessageRoutingCallback() {
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

                            String input = Optional.of(functionDefinition.input()).orElseThrow(() ->
                                new IllegalArgumentException("ConditionalFunctionBinding can be declared only with an input binding. "
                                    + "Please, consider using FunctionBinding."));

                            String condition = Optional.of(functionDefinition.condition()).orElseThrow(() ->
                                new IllegalArgumentException("ConditionalFunctionBinding can be declared only with a condition. "
                                    + "Please, consider using FunctionBinding."));

//                            Optional.ofNullable(functionDefinition.output())
//                                .filter(StringUtils::hasText)
//                                .ifPresent(output -> {
//                                    setOutput(getOutBinding(beanName), output, bindingServiceProperties, streamFunctionProperties, environment);
//                                });

                            String routerName = String.format(ROUTER_BEAN_NAME_PATTERN, input);

                            String routerCallbackName = String.format(ROUTER_CALLBACK_BEAN_NAME_PATTERN, routerName);

                            ConditionalMessageRoutingCallback callback = createRouterCallback(input, routerName, routerCallbackName,
                                beanFactory, functionDefinitionPropertySource, streamFunctionProperties);
                            callback.addRoutingExpression(beanName, condition);
                        });
                }

                return bean;
            }


        };
    }

    private FunctionCatalog getFunctionCatalog() {
        return applicationContext.getBean(FunctionCatalog.class);
    }

    private ConditionalMessageRoutingCallback createRouterCallback(String input, String routerName,
        String routerCallbackName, DefaultListableBeanFactory beanFactory, FunctionBindingPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties) {
        try {
            beanFactory.getBean(routerName, RoutingFunction.class);
            try {
                ConditionalMessageRoutingCallback callback = beanFactory.getBean(routerCallbackName,
                    ConditionalMessageRoutingCallback.class);
                return callback;
            } catch (BeansException e) {
                throw new IllegalStateException(
                    String.format("Router %s is defined, but it has no proper routing callback", routerName));
            }
        } catch (BeansException e) {
            ConditionalMessageRoutingCallback callback = createCallback(beanFactory, routerCallbackName);

            createRouter(beanFactory, routerName, callback);
            functionDefinitionPropertySource.register(routerName);
            setInput(getInBinding(routerName), input, streamFunctionProperties);
            return callback;
        }
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
