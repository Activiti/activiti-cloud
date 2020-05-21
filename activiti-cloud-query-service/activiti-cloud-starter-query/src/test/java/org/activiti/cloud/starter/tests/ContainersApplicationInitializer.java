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
package org.activiti.cloud.starter.tests;

import java.util.stream.Stream;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

public class ContainersApplicationInitializer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {


    private static GenericContainer keycloakContainer = new GenericContainer(
        "robfrank/activiti-keycloak")
        .withExposedPorts(8180)
        .waitingFor(Wait.defaultWaitStrategy());

    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
        "rabbitmq:management");

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        if (!keycloakContainer.isRunning() && !rabbitMQContainer.isRunning()) {
            Startables.deepStart(Stream.of(keycloakContainer, rabbitMQContainer)).join();
        }

        TestPropertyValues.of(
            "keycloak.auth-server-url=" + "http://" + keycloakContainer.getContainerIpAddress()
                + ":" + keycloakContainer.getFirstMappedPort() + "/auth",
            "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
            "spring.rabbitmq.port=" + String.valueOf(rabbitMQContainer.getAmqpPort())
        ).applyTo(context.getEnvironment());

    }
}
