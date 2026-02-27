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
package org.activiti.cloud.services.test.containers;

import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQContainerApplicationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.9-management-alpine")
        .withReuse(true)
        .withExposedPorts(5672, 5671, 15672, 15671);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        initialize();
        String vhostName = UUID.randomUUID().toString();
        createVhost(vhostName);
        context.addApplicationListener(
            (ApplicationListener<ContextClosedEvent>) event -> deleteVhost(vhostName)
        );
        TestPropertyValues.of(getContainerProperties(vhostName)).applyTo(context.getEnvironment());
    }

    public void initialize() {
        if (!rabbitMQContainer.isRunning()) {
            rabbitMQContainer.start();
        }
    }

    public static RabbitMQContainer getContainer() {
        return rabbitMQContainer;
    }

    public static String[] getContainerProperties() {
        return new String[] {
            "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort(),
        };
    }

    private static String[] getContainerProperties(String vhostName) {
        return new String[] {
            "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort(),
            "spring.rabbitmq.virtual-host=" + vhostName,
        };
    }

    private static void createVhost(String name) {
        try {
            Container.ExecResult addResult = rabbitMQContainer.execInContainer("rabbitmqctl", "add_vhost", name);
            if (addResult.getExitCode() != 0) {
                throw new RuntimeException(
                    "Failed to create RabbitMQ vhost '" + name + "': " + addResult.getStderr()
                );
            }
            Container.ExecResult permResult = rabbitMQContainer.execInContainer(
                "rabbitmqctl",
                "set_permissions",
                "-p",
                name,
                "guest",
                ".*",
                ".*",
                ".*"
            );
            if (permResult.getExitCode() != 0) {
                throw new RuntimeException(
                    "Failed to set permissions on RabbitMQ vhost '" + name + "': " + permResult.getStderr()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating RabbitMQ vhost: " + name, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create RabbitMQ vhost: " + name, e);
        }
    }

    private static void deleteVhost(String name) {
        if (rabbitMQContainer.isRunning()) {
            try {
                rabbitMQContainer.execInContainer("rabbitmqctl", "delete_vhost", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // best-effort cleanup
            }
        }
    }
}
