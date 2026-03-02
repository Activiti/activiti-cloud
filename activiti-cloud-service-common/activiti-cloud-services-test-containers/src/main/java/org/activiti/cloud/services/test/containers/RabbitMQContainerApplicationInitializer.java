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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

public class RabbitMQContainerApplicationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Object LOCK = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerApplicationInitializer.class);

    private static volatile boolean shutdownHookRegistered = false;
    private static volatile String currentVhostName;

    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.9-management-alpine")
        .withReuse(true)
        .withExposedPorts(5672, 5671, 15672, 15671);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        initialize();
        TestPropertyValues.of(getContainerProperties()).applyTo(context.getEnvironment());
    }

    public static void initialize() {
        synchronized (LOCK) {
            if (!rabbitMQContainer.isRunning()) {
                LOGGER.debug("Starting RabbitMQ Testcontainer...");
                rabbitMQContainer.start();
            }
            initializeVhost();
            registerShutdownHook();
        }
    }

    private static void initializeVhost() {
        currentVhostName = UUID.randomUUID().toString();
        createVhost(currentVhostName);
    }

    private static void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            synchronized (LOCK) {
                if (!shutdownHookRegistered) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteVhost(currentVhostName)));
                    shutdownHookRegistered = true;
                }
            }
        }
    }

    public static RabbitMQContainer getContainer() {
        return rabbitMQContainer;
    }

    public static String getCurrentVhostName() {
        return currentVhostName;
    }

    public static String[] getContainerProperties() {
        return new String[] {
            "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort(),
            "spring.rabbitmq.virtual-host=" + currentVhostName,
        };
    }

    private static void createVhost(String name) {
        try {
            Container.ExecResult addResult = rabbitMQContainer.execInContainer("rabbitmqctl", "add_vhost", name);
            if (addResult.getExitCode() != 0) {
                throw new RabbitMQContainerException(
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
                throw new RabbitMQContainerException(
                    "Failed to set permissions on RabbitMQ vhost '" + name + "': " + permResult.getStderr()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RabbitMQContainerException("Interrupted while creating RabbitMQ vhost: " + name, e);
        } catch (IOException e) {
            throw new RabbitMQContainerException("Failed to create RabbitMQ vhost: " + name, e);
        }
    }

    private static void deleteVhost(String name) {
        if (rabbitMQContainer.isRunning()) {
            try {
                rabbitMQContainer.execInContainer("rabbitmqctl", "delete_vhost", name);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while deleting RabbitMQ vhost '{}'", name, e);
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LOGGER.warn("Failed to delete RabbitMQ vhost '{}'", name, e);
            }
        }
    }
}
