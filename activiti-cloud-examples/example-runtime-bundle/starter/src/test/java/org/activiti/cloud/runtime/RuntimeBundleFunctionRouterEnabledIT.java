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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestPropertySource(
    properties = { "activiti.cloud.messaging.function-router.enabled=true", "activiti.cloud.application.name=myapp" }
)
@Testcontainers
public class RuntimeBundleFunctionRouterEnabledIT extends RuntimeBundleApplicationIT {

    @ServiceConnection
    @Container
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine");

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private Environment environment;

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput",
                "asyncExecutorJobsInput"
            );

        assertThat(bindingServiceProperties.getBindings())
            .containsOnlyKeys(
                "functionRouterInput",
                "asyncExecutorJobsOutput",
                "auditProducer",
                "commandResults",
                "messageConnectorOutput",
                "messageEventsOutput",
                "myCmdProducer",
                "signalProducer"
            );

        assertThat(bindingServiceProperties.getBindingProperties("functionRouterInput"))
            .extracting(BindingProperties::getGroup)
            .isEqualTo("my-runtime-bundle");
    }

    @Test
    void bindingServicePropertiesRequiredProducerGroups() {
        assertThat(bindingServiceProperties.getProducerProperties("signalProducer").getRequiredGroups()).isEmpty();
        assertThat(bindingServiceProperties.getProducerProperties("messageEventsOutput").getRequiredGroups()).isEmpty();
        assertThat(bindingServiceProperties.getProducerProperties("auditProducer").getRequiredGroups())
            .containsOnly("consumer");
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getFunctionRoutes())
            .containsOnly(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput",
                "asyncExecutorJobsInput"
            );

        assertThat(functionRouter.destinations())
            .containsOnlyKeys(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput",
                "asyncExecutorJobsInput"
            );

        assertThat(functionRouter.registrations())
            .containsOnlyKeys(
                "commandConsumer_myapp",
                "asyncExecutorJobs_myapp",
                "messageEvents_myapp",
                "integrationResult_my-runtime-bundle",
                "integrationError_my-runtime-bundle",
                "signalEvent"
            );
    }

    @Test
    void environment() {
        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.functionRouterInput.consumer.queue-name-group-only",
                Boolean.class
            )
        )
            .isTrue();

        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.auditProducer.producer.queue-name-group-only",
                Boolean.class
            )
        )
            .isTrue();
    }
}
