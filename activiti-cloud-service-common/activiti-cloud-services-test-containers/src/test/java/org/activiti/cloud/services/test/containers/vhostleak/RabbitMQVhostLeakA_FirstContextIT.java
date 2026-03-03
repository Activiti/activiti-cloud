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

import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.TestContainersApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * First context in the vhost leak scenario.
 * Uses property "test.context=first" to ensure Spring creates a unique context.
 * Records the vhost created by the initializer into {@link VhostLeakTestState}.
 *
 * <p>Alphabetical class name (A) ensures this runs before the B counterpart.</p>
 */
@SpringBootTest(
    classes = TestContainersApplication.class,
    properties = "test.context=first"
)
@ContextConfiguration(initializers = RabbitMQContainerApplicationInitializer.class)
class RabbitMQVhostLeakA_FirstContextIT {

    @Test
    void shouldRecordVhostFromFirstContext() {
        String vhost = RabbitMQContainerApplicationInitializer.getCurrentVhostName();

        assertThat(vhost)
            .as("First context should have a vhost assigned")
            .isNotNull()
            .isNotEmpty();

        // Store for the second context to verify the leak
        VhostLeakTestState.firstContextVhost = vhost;
    }
}
