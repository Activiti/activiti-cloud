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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.testcontainers.containers.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(classes = { TestContainersApplication.class })
@ContextConfiguration(initializers = { RabbitMQContainerApplicationInitializer.class })
@TestExecutionListeners(
    value = RabbitMQQueuesCleanupTestExecutionListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RabbitMQQueuesCleanupTestExecutionListenerIT {

    private static final String TEST_QUEUE = "cleanup-listener-test-queue";

    @Autowired
    private Environment environment;

    private String vhost() {
        return environment.getProperty("spring.rabbitmq.virtual-host");
    }

    @Test
    @Order(1)
    void shouldHaveMessagesInQueueBeforeListenerRuns() throws IOException, InterruptedException {
        RabbitMQContainer container = RabbitMQContainerApplicationInitializer.getContainer();
        String vhost = vhost();

        declareQueue(container, vhost);
        publishMessage(container, vhost, "message-1");
        publishMessage(container, vhost, "message-2");

        assertThat(getMessageCount(container, vhost))
            .as("Queue should contain messages before cleanup listener runs")
            .isEqualTo(2);
    }

    @Test
    @Order(2)
    void shouldHaveQueuePurgedAfterPreviousTest() throws IOException, InterruptedException {
        RabbitMQContainer container = RabbitMQContainerApplicationInitializer.getContainer();
        String vhost = vhost();

        // The listener ran after @Order(1) — queue must be empty
        assertThat(getMessageCount(container, vhost))
            .as("Queue should be empty after cleanup listener ran following the previous test")
            .isZero();
    }

    @Test
    @Order(3)
    void shouldPurgeMessagesPublishedInCurrentTestAsWell() throws IOException, InterruptedException {
        RabbitMQContainer container = RabbitMQContainerApplicationInitializer.getContainer();
        String vhost = vhost();

        publishMessage(container, vhost, "message-3");

        assertThat(getMessageCount(container, vhost))
            .as("Queue should contain the newly published message in this test")
            .isEqualTo(1);
        // After this test method the listener will purge again; @Order(4) verifies it
    }

    @Test
    @Order(4)
    void shouldHaveQueuePurgedAfterThirdTest() throws IOException, InterruptedException {
        RabbitMQContainer container = RabbitMQContainerApplicationInitializer.getContainer();
        String vhost = vhost();

        assertThat(getMessageCount(container, vhost))
            .as("Queue should be empty after cleanup listener ran following the third test")
            .isZero();
    }

    private static void declareQueue(RabbitMQContainer container, String vhost)
        throws IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer(
            "rabbitmqadmin",
            "--vhost=" + vhost,
            "declare",
            "queue",
            "name=" + TEST_QUEUE,
            "durable=false"
        );
        assertThat(result.getExitCode()).as("rabbitmqadmin declare queue stderr: " + result.getStderr()).isZero();
    }

    private static void publishMessage(RabbitMQContainer container, String vhost, String payload)
        throws IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer(
            "rabbitmqadmin",
            "--vhost=" + vhost,
            "publish",
            "exchange=amq.default",
            "routing_key=" + TEST_QUEUE,
            "payload=" + payload
        );
        assertThat(result.getExitCode()).as("rabbitmqadmin publish stderr: " + result.getStderr()).isZero();
    }

    private static int getMessageCount(RabbitMQContainer container, String vhost)
        throws IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer(
            "rabbitmqctl",
            "list_queues",
            "name",
            "messages",
            "--no-table-headers",
            "-p",
            vhost
        );
        assertThat(result.getExitCode()).as("rabbitmqctl list_queues stderr: " + result.getStderr()).isZero();

        return result
            .getStdout()
            .lines()
            .filter(line -> line.trim().startsWith(TEST_QUEUE))
            .mapToInt(line -> {
                String[] parts = line.trim().split("\\s+");
                return parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            })
            .sum();
    }
}
