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
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.rabbitmq.RabbitMQContainer;

public class RabbitMQContainerApplicationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{

    private static final Object LOCK = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerApplicationInitializer.class);

    private static final ConcurrentLinkedQueue<String> trackedVhosts = new ConcurrentLinkedQueue<>();

    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.8.9-management-alpine")
        .withReuse(true)
        .withExposedPorts(5672, 5671, 15672, 15671);
    protected static final String SET_PERMISSIONS_COMMAND =
        "rabbitmqctl set_permissions --vhost %s guest '.*' '.*' '.*'";
    protected static final String ADD_VHOST_COMMAND = "rabbitmqctl add_vhost %s";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(initialize()).applyTo(context.getEnvironment());
    }

    public static String[] initialize() {
        synchronized (LOCK) {
            if (!rabbitMQContainer.isRunning()) {
                LOGGER.debug("Starting RabbitMQ Testcontainer...");
                rabbitMQContainer.start();
                registerShutdownHook();
            }
            return initializeVhost();
        }
    }

    private static String[] initializeVhost() {
        String vhostName = UUID.randomUUID().toString();
        createVhost(vhostName);
        trackedVhosts.add(vhostName);
        return buildProperties(vhostName);
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                String vhost;
                while ((vhost = trackedVhosts.poll()) != null) {
                    deleteVhost(vhost);
                }
            })
        );
    }

    private static String[] buildProperties(String vhostName) {
        return new String[] {
            "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
            "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort(),
            "spring.rabbitmq.virtual-host=" + vhostName,
        };
    }

    public static RabbitMQContainer getContainer() {
        return rabbitMQContainer;
    }

    public static Collection<String> getTrackedVhosts() {
        return Collections.unmodifiableCollection(trackedVhosts);
    }

    private static void createVhost(String name) {
        try {
            Container.ExecResult addResult = executeInContainer(ADD_VHOST_COMMAND.formatted(name));
            if (addResult.getExitCode() != 0) {
                throw new RabbitMQContainerException(
                    "Failed to create RabbitMQ vhost '" + name + "': " + addResult.getStderr()
                );
            }
            Container.ExecResult permResult = executeInContainer(SET_PERMISSIONS_COMMAND.formatted(name));
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

    private static ExecResult executeInContainer(String command) throws IOException, InterruptedException {
        return rabbitMQContainer.execInContainer("sh", "-c", command);
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
