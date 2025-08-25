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
package org.activiti.cloud.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestApplication.class },
    properties = { "activiti.cloud.messaging.function-router.enabled=true" }
)
@Testcontainers
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
public class QueryRestApplicationFunctionRouterIT {

    @ServiceConnection
    @Container
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Test
    void contextLoads() {}

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings())
            .doesNotContainKeys("auditConsumer", "queryConsumer")
            .containsOnlyKeys("functionRouterAnonymousInput", "producer");
    }

    @Test
    void functionRouter() {
        var functionRouter = messagingProperties.getFunctionRouter();

        assertThat(functionRouter.isEnabled()).isTrue();

        assertThat(functionRouter.getRoutes())
            .containsOnlyKeys("auditConsumer", "queryConsumer", "graphQLEngineEventsConsumerSource");

        assertThat(functionRouter.destinations())
            .containsOnly(Map.entry("graphQLEngineEventsConsumerSource", "engineEvents"));

        assertThat(functionRouter.registrations())
            .containsOnlyKeys("engineEvents")
            .satisfies(registrations ->
                assertThat(registrations.get("engineEvents"))
                    .containsOnly("engineEventsGraphQlSourceConsumer_registration")
                    .isNotEmpty()
            );
    }
}
