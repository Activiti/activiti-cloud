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

import com.rabbitmq.client.ConnectionFactory;
import java.util.Optional;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableConfigurationProperties(ActivitiCloudMessagingProperties.class)
@PropertySource("classpath:config/activiti-cloud-messaging.properties")
@PropertySource(value = "file:config/activiti-cloud-messaging.properties", ignoreResourceNotFound = true)
public class ActivitiCloudMessagingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ConnectionFactory.class, MessageListenerContainer.class })
    static class ActivitiCloudMessagingRabbitConfiguration {

        @Bean
        ListenerContainerCustomizer<MessageListenerContainer> activitiRabbitMqMessageListenerContainerCustomizer(
            ActivitiCloudMessagingProperties activitiCloudMessagingProperties
        ) {
            return (container, destinationName, group) -> {
                if (container instanceof AbstractMessageListenerContainer rabbitListenerContainer) {
                    if (group == null) {
                        Optional
                            .ofNullable(activitiCloudMessagingProperties)
                            .map(ActivitiCloudMessagingProperties::getRabbitmq)
                            .map(ActivitiCloudMessagingProperties.RabbitMqProperties::getMissingAnonymousQueuesFatal)
                            .ifPresent(rabbitListenerContainer::setMissingQueuesFatal);
                    }
                }
            };
        }
    }
}
