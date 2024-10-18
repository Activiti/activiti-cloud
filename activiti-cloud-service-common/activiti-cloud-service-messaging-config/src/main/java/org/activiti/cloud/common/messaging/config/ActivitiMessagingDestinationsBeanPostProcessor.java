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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.Ordered;

public class ActivitiMessagingDestinationsBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ActivitiMessagingDestinationsBeanPostProcessor.class);

    private final ActivitiMessagingDestinationTransformer destinationTransformer;

    public ActivitiMessagingDestinationsBeanPostProcessor(
        ActivitiMessagingDestinationTransformer destinationTransformer
    ) {
        this.destinationTransformer = destinationTransformer;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (BindingServiceProperties.class.isInstance(bean)) {
            BindingServiceProperties bindingServiceProperties = BindingServiceProperties.class.cast(bean);

            log.info("Post-processing messaging destinations for bean {} with name {}", bean, beanName);

            bindingServiceProperties
                .getBindings()
                .forEach((bindingName, bindingProperties) -> {
                    String source = Optional.ofNullable(bindingProperties.getDestination()).orElse(bindingName);

                    String destination = destinationTransformer.apply(source);

                    bindingProperties.setDestination(destination);

                    log.warn("Configured destination '{}' for binding '{}'", destination, bindingName);
                });
        }

        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
