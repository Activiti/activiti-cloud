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

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@ConditionalOnClass(BindingServiceProperties.class)
@ConditionalOnProperty(prefix = "activiti.cloud.messaging",
                       name = "destination-override-enabled",
                       havingValue = "true",
                       matchIfMissing = false)
public class ActivitiMessagingDestinationsBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(ActivitiMessagingDestinationsBeanPostProcessor.class);

    private final ActivitiCloudMessagingProperties messagingProperties;
    private final ConfigurableEnvironment environment;

    public ActivitiMessagingDestinationsBeanPostProcessor(ActivitiCloudMessagingProperties messagingProperties,
                                                          ConfigurableEnvironment environment) {
        this.messagingProperties = messagingProperties;
        this.environment = environment;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (BindingServiceProperties.class.isInstance(bean)) {
            BindingServiceProperties bindingServiceProperties = BindingServiceProperties.class.cast(bean);

            log.info("Post-processing bean {} with name {}", bean, beanName);

            messagingProperties.getDestinations()
                               .entrySet()
                               .forEach(destinationEntry -> {
                          String destinationKey = destinationEntry.getKey();
                          ActivitiCloudMessagingProperties.DestinationProperties destinationProperties = destinationEntry.getValue();

                          String[] bindings = Optional.ofNullable(destinationProperties.getBindings())
                                                      .orElseGet(() -> new String[] {destinationEntry.getKey()});
                          String scope = Optional.ofNullable(destinationProperties.getScope())
                                                 .orElseGet(destinationEntry::getKey);
                          String prefix = Optional.ofNullable(destinationProperties.getPrefix())
                                                  .orElseGet(messagingProperties::getDestinationPrefix);
                          String separator = Optional.ofNullable(destinationProperties.getSeparator())
                                                     .orElseGet(messagingProperties::getDestinationSeparator);

                          log.info("Found destination key '{}' for bindings '{}' with prefix '{}' and scope '{}' using separator '{}'",
                                   destinationKey,
                                   bindings,
                                   prefix,
                                   scope,
                                   separator);

                          Stream.concat(Stream.of(destinationKey), Stream.of(bindings))
                              .forEach(binding -> {
                                  BindingProperties bindingProperties = bindingServiceProperties.getBindingProperties(binding);

                                  if (bindingProperties != null) {
                                      String destination = buildDestination(scope,
                                                                            prefix,
                                                                            separator);

                                      log.info("Overriding destination '{}' for binding '{}'",
                                                destination,
                                                binding);

                                      bindingProperties.setDestination(destination);
                                  }
                              });
                      });
        }

        return bean;
    }

    private void applyDestination(ActivitiCloudMessagingProperties.DestinationProperties destinationProperties) {

    }

    private String buildDestination(String scope,
                                    String prefix,
                                    String separator) {
        StringBuilder value = new StringBuilder();

        if (StringUtils.hasText(prefix)) {
            value.append(prefix)
                 .append(separator);
        }

        value.append(scope);

        return value.toString();
    }
}
