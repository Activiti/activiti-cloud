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

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties.MessagingBroker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class ActivitiMessagingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    protected static final String ACTIVITI_CLOUD_MESSAGING_BROKER_KEY = "activiti.cloud.messaging.broker";
    protected static final String SPRING_CLOUD_STREAM_DEFAULT_BINDER_KEY = "spring.cloud.stream.default-binder";
    protected static final String MANAGEMENT_HEALTH_RABBIT_ENABLED_KEY = "management.health.rabbit.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        final MessagingBroker messagingBroker = environment.getProperty(
            ACTIVITI_CLOUD_MESSAGING_BROKER_KEY,
            MessagingBroker.class,
            MessagingBroker.rabbitmq
        );

        environment
            .getPropertySources()
            .addAfter(
                SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(this.getClass().getSimpleName(), resolvePropertiesToSet(messagingBroker))
            );
    }

    private Map<String, Object> resolvePropertiesToSet(MessagingBroker messagingBroker) {
        Map<String, Object> extraProperties = new HashMap<>();
        if (MessagingBroker.kafka.equals(messagingBroker)) {
            extraProperties.put(MANAGEMENT_HEALTH_RABBIT_ENABLED_KEY, false);
        }
        extraProperties.put(SPRING_CLOUD_STREAM_DEFAULT_BINDER_KEY, resolveDefaultBinder(messagingBroker));
        return extraProperties;
    }

    private String resolveDefaultBinder(MessagingBroker messagingBroker) {
        switch (messagingBroker) {
            case kafka:
                return "kafka";
            case rabbitmq:
            default:
                return "rabbit";
        }
    }
}
