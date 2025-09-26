/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

@AutoConfiguration(after = BinderFactoryAutoConfiguration.class, before = FunctionConfiguration.class)
public class InputBindingConfiguration extends AbstractFunctionalBindingConfiguration {

    public static final String INPUT_BINDING = "_sink";
    private static final Logger log = LoggerFactory.getLogger(InputBindingConfiguration.class);

    @Bean
    public BeanPostProcessor inputBindingBeanPostProcessor(
        FunctionAnnotationService functionAnnotationService,
        BindingServiceProperties bindingServiceProperties,
        StreamFunctionProperties streamFunctionProperties,
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (MessageChannel.class.isInstance(bean)) {
                    final var functionRouter = messagingProperties.getFunctionRouter();

                    Optional
                        .ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, InputBinding.class))
                        .filter(inputBinding -> {
                            final var hasBindingConfiguration = bindingServiceProperties
                                .getBindings()
                                .containsKey(inputBinding.value()[0]);

                            if (!hasBindingConfiguration) {
                                log.warn(
                                    "Skipping input binding {} configuration due to missing binding service properties",
                                    inputBinding.value()[0]
                                );
                            }

                            return hasBindingConfiguration;
                        })
                        .ifPresent(inputBinding -> {
                            final String beanInName = getInBinding(beanName + INPUT_BINDING);

                            String inputBindings = bindingServiceProperties.getInputBindings();

                            if (!StringUtils.hasText(inputBindings)) {
                                inputBindings = beanInName;
                            } else {
                                inputBindings += ";" + beanInName;
                            }

                            bindingServiceProperties.setInputBindings(inputBindings);

                            streamFunctionProperties.getBindings().put(beanInName, beanName);

                            if (!DirectWithAttributesChannel.class.isInstance(bean)) {
                                getMessageConverterConfigurer()
                                    .configureInputChannel(MessageChannel.class.cast(bean), beanName);
                            }
                        });
                }

                return bean;
            }
        };
    }
}
