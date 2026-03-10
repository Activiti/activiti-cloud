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
import org.testcontainers.containers.Container.ExecResult;
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
        if (vhost == null || vhost.isEmpty()) {
            LOGGER.warn(
                "RabbitMQ virtual host property 'spring.rabbitmq.virtual-host' is not set; skipping queue purge"
            );
            return;
        }
        try {
            // Execute a single shell command in the container that lists all queues for the vhost
            // and purges each of them. This avoids one Docker exec per queue.
            var purgeResult = executePurgeAllQueues(vhost, container);
            if (purgeResult.getExitCode() != 0) {
                LOGGER.warn("Failed to purge RabbitMQ queues for vhost '{}': {}", vhost, purgeResult.getStderr());
            } else {
                LOGGER.debug("Purged all RabbitMQ queues for vhost '{}'", vhost);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while purging RabbitMQ queues for vhost '{}'", vhost, e);
        } catch (IOException e) {
            LOGGER.warn("Failed to purge RabbitMQ queues for vhost '{}'", vhost, e);
        }
    }

    private static ExecResult executePurgeAllQueues(String vhost, RabbitMQContainer container)
        throws IOException, InterruptedException {
        String purgeCommand = String.format(
            "rabbitmqctl list_queues name --no-table-headers -p '%s' | grep -vE '^\\s*(Timeout|Listing|$)' | while read q; do rabbitmqctl purge_queue \"$q\" -p '%s'; done",
            vhost,
            vhost
        );
        return container.execInContainer("sh", "-c", purgeCommand);
    }
}
