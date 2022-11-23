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

import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@AutoConfigureBefore(BinderFactoryAutoConfiguration.class)
@ConditionalOnClass(BindingServiceProperties.class)
public class FunctionBindingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionBindingConfiguration.class);

    @Bean
    public FunctionBindingPropertySource functionDefinitionPropertySource(ConfigurableApplicationContext applicationContext) {
        return new FunctionBindingPropertySource(applicationContext.getEnvironment());
    }

    @Bean
    public ChannelResolver channelResolver(ApplicationContext context) {
        return channelName -> context.getBean(channelName, MessageChannel.class);
    }

    @Bean
    public BeanPostProcessor functionBindingBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        FunctionBindingPropertySource functionDefinitionPropertySource,
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Supplier.class.isInstance(bean) ||
                    Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)) {

                    Optional.ofNullable(findAnnotationOnBean(beanName, beanFactory))
                        .ifPresent(functionDefinition -> {
                            functionDefinitionPropertySource.register(beanName);

                            final String beanInName = FunctionalBindingHelper.getInBinding(beanName);
                            final String beanOutName = FunctionalBindingHelper.getOutBinding(beanName);

                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    Optional.ofNullable(bindingServiceProperties.getBindingDestination(output))
                                        .ifPresentOrElse(
                                            binding -> streamFunctionProperties.getBindings()
                                                .put(beanOutName, binding),
                                            () -> streamFunctionProperties.getBindings()
                                                .put(beanOutName, output)
                                        );

                                    Optional.ofNullable(bindingServiceProperties.getProducerProperties(output))
                                        .ifPresent(producerProperties -> {
                                            bindingServiceProperties.getBindingProperties(beanOutName).setProducer(producerProperties);
                                        });
                                });

                            Optional.of(functionDefinition.input())
                                .filter(StringUtils::hasText)
                                .ifPresent(input -> {
                                    streamFunctionProperties.getBindings()
                                        .put(beanInName, input);
                                });
                        });
                }

                return bean;
            }
        };
    }

    @Nullable
    private FunctionBinding findAnnotationOnBean(String beanName, DefaultListableBeanFactory beanFactory) {
        try {
            return beanFactory.findAnnotationOnBean(beanName, FunctionBinding.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("Bean with name {} not found.", beanName);
            return null;
        }
    }

    @FunctionalInterface
    public interface ChannelResolver {
        MessageChannel resolveDestination(String channelName);
    }

}
