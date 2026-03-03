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
package org.activiti.cloud.services.test.containers.vhostleak;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.TestContainersApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Second context in the vhost leak scenario.
 * Uses property "test.context=second" to force a different Spring context than the A class.
 *
 * <p>Verifies the desired behavior: all vhosts created across multiple Spring contexts
 * are tracked by the initializer and will all be cleaned up at JVM shutdown.</p>
 *
 * <p>Alphabetical class name (B) ensures this runs after the A counterpart.</p>
 */
@SpringBootTest(
    classes = TestContainersApplication.class,
    properties = "test.context=second"
)
@ContextConfiguration(initializers = RabbitMQContainerApplicationInitializer.class)
class RabbitMQVhostLeakB_SecondContextIT {

    @Test
    void shouldTrackAllVhostsAcrossMultipleContexts() throws IOException, InterruptedException {
        String firstVhost = VhostLeakTestState.firstContextVhost;
        String secondVhost = RabbitMQContainerApplicationInitializer.getCurrentVhostName();

        assertThat(firstVhost)
            .as("First context test must have run before this one")
            .isNotNull();

        assertThat(secondVhost)
            .as("Second context should have created a different vhost")
            .isNotEqualTo(firstVhost);

        // Both vhosts should be tracked — the shutdown hook will delete all of them
        Collection<String> trackedVhosts = RabbitMQContainerApplicationInitializer.getTrackedVhosts();

        assertThat(trackedVhosts)
            .as("All vhosts created across multiple contexts must be tracked for cleanup")
            .contains(firstVhost, secondVhost);

            Thread.sleep(10000);
    }
}
