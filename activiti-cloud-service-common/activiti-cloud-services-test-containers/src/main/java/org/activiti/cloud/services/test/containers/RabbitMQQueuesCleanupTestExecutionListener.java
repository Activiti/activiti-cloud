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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.containers.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

public class RabbitMQQueuesCleanupTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQQueuesCleanupTestExecutionListener.class);

    @Override
    public void afterTestMethod(TestContext testContext) {
        purgeAllQueues(testContext);
    }

    private static void purgeAllQueues(TestContext testContext) {
        RabbitMQContainer container = RabbitMQContainerApplicationInitializer.getContainer();
        if (!container.isRunning()) {
            return;
        }
        String vhost = testContext.getApplicationContext().getEnvironment().getProperty("spring.rabbitmq.virtual-host");
        try {
            Container.ExecResult listResult = container.execInContainer(
                "rabbitmqctl",
                "list_queues",
                "name",
                "--no-table-headers",
                "-p",
                vhost
            );
            if (listResult.getExitCode() != 0) {
                LOGGER.warn("Failed to list RabbitMQ queues: {}", listResult.getStderr());
                return;
            }
            String output = listResult.getStdout().trim();
            if (output.isEmpty()) {
                return;
            }
            for (String queueName : output.split("\\n")) {
                queueName = queueName.trim();
                if (!queueName.isEmpty()) {
                    purgeQueue(container, vhost, queueName);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while listing RabbitMQ queues", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to list RabbitMQ queues", e);
        }
    }

    private static void purgeQueue(RabbitMQContainer container, String vhost, String queueName) {
        try {
            Container.ExecResult purgeResult = container.execInContainer(
                "rabbitmqctl",
                "purge_queue",
                queueName,
                "-p",
                vhost
            );
            if (purgeResult.getExitCode() != 0) {
                LOGGER.warn("Failed to purge RabbitMQ queue '{}': {}", queueName, purgeResult.getStderr());
            } else {
                LOGGER.debug("Purged RabbitMQ queue '{}'", queueName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while purging RabbitMQ queue '{}'", queueName, e);
        } catch (IOException e) {
            LOGGER.warn("Failed to purge RabbitMQ queue '{}'", queueName, e);
        }
    }
}
