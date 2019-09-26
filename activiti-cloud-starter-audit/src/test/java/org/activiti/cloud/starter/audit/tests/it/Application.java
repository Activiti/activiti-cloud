/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.starter.audit.tests.it;

import org.activiti.cloud.starter.audit.configuration.EnableActivitiAudit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.util.stream.Stream;

@SpringBootApplication
@EnableActivitiAudit
@ComponentScan({"org.activiti.cloud.starters.test",
        "org.activiti.cloud.starter.audit.tests.it",
        "org.activiti.cloud.services.test.identity.keycloak.interceptor"})
public class Application {

    static GenericContainer keycloakContainer = new GenericContainer("activiti/activiti-keycloak")
            .withExposedPorts(8180)
            .waitingFor(Wait.defaultWaitStrategy());

    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:management");

    static {
        Startables.deepStart(Stream.of(keycloakContainer, rabbitMQContainer)).join();

        System.setProperty("keycloak.auth-server-url", "http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getFirstMappedPort() + "/auth");
        System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getContainerIpAddress());
        System.setProperty("spring.rabbitmq.port", String.valueOf(rabbitMQContainer.getAmqpPort()));

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class,
                args);
    }
}
