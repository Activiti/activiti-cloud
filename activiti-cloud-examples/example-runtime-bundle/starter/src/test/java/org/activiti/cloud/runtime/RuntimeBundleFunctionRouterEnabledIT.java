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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = RuntimeBundleApplication.class,
    properties = { "activiti.cloud.messaging.function-router.enabled=true", "activiti.cloud.application.name=myapp" }
)
@ContextConfiguration(
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@Testcontainers
public class RuntimeBundleFunctionRouterEnabledIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput"
            )
            .containsKey("functionRouterInput");
    }

    @Test
    void messagingProperties() {
        var functionRouterDestinations = messagingProperties
            .getDestinations()
            .entrySet()
            .stream()
            .filter(it -> it.getValue().isFunctionRouter())
            .map(Map.Entry::getKey)
            .toList();

        assertThat(functionRouterDestinations)
            .containsOnly(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput"
            );
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getBindings())
            .containsOnly(
                "commandConsumer",
                "integrationErrorsConsumer",
                "integrationResultsConsumer",
                "myCmdResults",
                "signalConsumer",
                "messageConnectorInput"
            );
    }

    @Test
    void environment() {
        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.messageEventsOutput.producer.bind-queue",
                Boolean.class
            )
        )
            .isFalse();
        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.signalProducer.producer.bind-queue",
                Boolean.class
            )
        )
            .isFalse();

        assertThat(environment.getProperty("activiti.cloud.messaging.function-router.bindings", String.class)).isNull();

        assertThat(
            environment.getProperty(
                "spring.cloud.stream.rabbit.bindings.functionRouterInput.consumer.queue-name-group-only",
                Boolean.class
            )
        )
            .isTrue();

        assertThat(environment.getProperty("activiti.cloud.messaging.function-router.group", String.class))
            .isEqualTo("my-runtime-bundle");
    }
}
