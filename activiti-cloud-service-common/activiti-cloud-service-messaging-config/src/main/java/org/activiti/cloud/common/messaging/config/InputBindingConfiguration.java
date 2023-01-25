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

import java.util.Optional;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.binding.MessageConverterConfigurer;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import static org.activiti.cloud.common.messaging.config.AbstractFunctionalBindingConfiguration.getInBinding;

@Configuration
public class InputBindingConfiguration {

    @Bean
    public BeanPostProcessor inputBindingBeanPostProcessor(DefaultListableBeanFactory beanFactory,
                                                           BindingServiceProperties bindingServiceProperties,
                                                           StreamFunctionProperties streamFunctionProperties,
                                                           MessageConverterConfigurer messageConverterConfigurer) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (MessageChannel.class.isInstance(bean)) {

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, InputBinding.class))
                            .ifPresent(functionBinding -> {
                                final String inputBinding = beanName + "Function";
                                final String beanInName = getInBinding(inputBinding);

                                String inputBindings = bindingServiceProperties.getInputBindings();

                                if (!StringUtils.hasText(inputBindings)) {
                                    inputBindings = inputBinding;
                                } else {
                                    inputBindings += ";" + inputBinding;
                                }

                                bindingServiceProperties.setInputBindings(inputBindings);

                                streamFunctionProperties.getBindings()
                                                        .put(beanInName, beanName);

                                if (!DirectWithAttributesChannel.class.isInstance(bean)) {
                                    messageConverterConfigurer.configureInputChannel(MessageChannel.class.cast(bean),
                                                                                     beanName);
                                }
                            });
                }

                return bean;
            }
        };
    }

}
