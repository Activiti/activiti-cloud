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

import java.util.Collection;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.TestContainersApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Second Spring context in the multi-context vhost tracking scenario.
 *
 * <p>These tests are disabled and must be run manually together with
 * {@link RabbitMQVhostLeakA_FirstContextIT} to verify that all vhosts created across
 * multiple Spring contexts in the same JVM are correctly tracked for cleanup.
 *
 * <p>To run manually:
 * <pre>
 * mvn verify -pl activiti-cloud-service-common/activiti-cloud-services-test-containers \
 *   -Dfailsafe.runOrder=alphabetical \
 *   -Dit.test="RabbitMQVhostLeak*"
 * </pre>
 *
 * <p>Alphabetical class name (B) ensures this runs after the A counterpart.
 */
@Disabled("Run manually to verify multi-context vhost tracking and cleanup")
@SpringBootTest(classes = TestContainersApplication.class, properties = "test.context=second")
@ContextConfiguration(initializers = RabbitMQContainerApplicationInitializer.class)
class RabbitMQVhostLeakB_SecondContextIT {

    @Test
    void shouldTrackAllVhostsAcrossMultipleContexts() {
        // Both vhosts should be tracked — the shutdown hook will delete all of them
        Collection<String> trackedVhosts = RabbitMQContainerApplicationInitializer.getTrackedVhosts();

        assertThat(trackedVhosts)
            .as("All vhosts created across multiple contexts must be tracked for cleanup")
            .hasSize(2);
    }
}
