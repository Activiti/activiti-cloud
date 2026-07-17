/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@TestPropertySource(
    properties = {
        "activiti.cloud.application.name=default-app",
        "activiti.cloud.messaging.rabbitmq.prefix=${activiti.cloud.application.name}.",
    }
)
public class RuntimeBundleRabbitmqPrefixIT extends RuntimeBundleApplicationIT {

    @ServiceConnection
    @Container
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine");

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private Environment environment;

    @Test
    @Override
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues())
            .isNotEmpty()
            .containsOnlyKeys(
                "default-app.engineEvents.query",
                "default-app.engineEvents.audit",
                "default-app.messageEvents_default-app.messages",
                "default-app.signalEvent.my-runtime-bundle",
                "default-app.commandConsumer_default-app.my-runtime-bundle",
                "default-app.asyncExecutorJobs_default-app.my-runtime-bundle",
                "default-app.integrationResult_my-runtime-bundle.my-runtime-bundle",
                "default-app.integrationError_my-runtime-bundle.my-runtime-bundle",
                "default-app.connectorIncident.my-runtime-bundle"
            );
    }

    @Test
    @Override
    void rabbitExchanges() {
        assertThat(binderFactoryListenerTestContext.getExchanges())
            .isNotEmpty()
            .containsOnlyKeys(
                "default-app.commandResults_default-app",
                "default-app.engineEvents",
                "default-app.asyncExecutorJobs_default-app",
                "default-app.commandConsumer_default-app",
                "default-app.messageEvents_default-app",
                "default-app.signalEvent",
                "default-app.integrationResult_my-runtime-bundle",
                "default-app.integrationError_my-runtime-bundle",
                "default-app.connectorIncident"
            );
    }

    @Test
    @Override
    void messagingRabbitMqPrefixProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isEqualTo("default-app.");
    }

    @Test
    @Override
    void rabbitBinderDefaultPrefix() {
        assertThat(
            environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class)
        ).isEqualTo("default-app.");

        assertThat(
            environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class)
        ).isEqualTo("default-app.");
    }
}
