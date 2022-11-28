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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@AutoConfigureBefore(BinderFactoryAutoConfiguration.class)
@ConditionalOnClass(BindingServiceProperties.class)
public class FunctionBindingConfiguration {

    @Bean
    public FunctionBindingPropertySource functionDefinitionPropertySource(ConfigurableApplicationContext applicationContext) {
        return new FunctionBindingPropertySource(applicationContext.getEnvironment());
    }

    @Bean
    public FunctionAnnotationService functionAnnotationService(DefaultListableBeanFactory beanFactory) {
        return new FunctionAnnotationService(beanFactory);
    }

    @Bean
    public BeanPostProcessor functionBindingBeanPostProcessor(FunctionAnnotationService functionAnnotationService,
                                                              FunctionBindingPropertySource functionDefinitionPropertySource,
                                                              StreamFunctionProperties streamFunctionProperties,
                                                              BindingServiceProperties bindingServiceProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (Supplier.class.isInstance(bean) ||
                    Function.class.isInstance(bean) ||
                    Consumer.class.isInstance(bean)) {

                    Optional.ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, FunctionBinding.class))
                        .ifPresent(functionDefinition -> {
                            functionDefinitionPropertySource.register(beanName);

                            final String beanInName = FunctionalBindingHelper.getInBinding(beanName);
                            final String beanOutName = FunctionalBindingHelper.getOutBinding(beanName);

                            Optional.of(functionDefinition.output())
                                .filter(StringUtils::hasText)
                                .ifPresent(output -> {
                                    Optional.ofNullable(bindingServiceProperties.getBindingDestination(output))
                                        .ifPresentOrElse(
                                            binding -> setOutProperties(streamFunctionProperties, beanOutName,
                                                    binding, bindingServiceProperties, output),
                                            () -> setOutProperties(streamFunctionProperties, beanOutName,
                                                    output, bindingServiceProperties, output)
                                        );
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

    private void setOutProperties(StreamFunctionProperties streamFunctionProperties,
                                  String beanOutName,
                                  String binding,
                                  BindingServiceProperties bindingServiceProperties,
                                  String functionDefinitionOutput) {
        streamFunctionProperties.getBindings().put(beanOutName, binding);
        Optional.ofNullable(bindingServiceProperties.getProducerProperties(functionDefinitionOutput))
                .ifPresent(producerProperties -> {
                    bindingServiceProperties.getBindingProperties(beanOutName).setProducer(producerProperties);
                    bindingServiceProperties.getBindingProperties(binding).setProducer(producerProperties);
                });
    }

}
