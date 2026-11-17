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
package org.activiti.cloud.common.messaging.config.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;
import javax.sql.DataSource;
import org.activiti.cloud.common.messaging.outbox.OutboxLifecycleController;
import org.activiti.cloud.common.messaging.outbox.OutboxManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.outbox.enabled=true", "spring.sql.init.platform=h2" })
@Import(FunctionBindingOutboxConfigurationIT.SpringIntegrationJdbcConfiguration.class)
public class FunctionBindingOutboxConfigurationIT extends FunctionBindingConfigurationIT {

    @Autowired
    private OutboxManager outboxManager;

    @Autowired
    private OutboxLifecycleController outboxLifecycleController;

    @TestConfiguration
    static class SpringIntegrationJdbcConfiguration {

        @Bean
        public LockRepository lockRepository(DataSource dataSource) {
            return new DefaultLockRepository(dataSource);
        }

        @Bean
        LockRegistry lockRegistry(LockRepository lockRepository) {
            return new JdbcLockRegistry(lockRepository);
        }
    }

    @Test
    void outboxLifecycleController_allEndpointsRunning() {
        assertThat(outboxLifecycleController.allEndpointsRunning()).isTrue();
    }

    @Test
    void outboxLifecycleController_getEndpoints() {
        assertThat(outboxLifecycleController.getEndpoints()).containsOnly(
            "commandResultsOutbox",
            "integrationResultsOutbox",
            "auditProducerIncidentsOutbox",
            "streamBridgeOutbox",
            "auditProducerOutbox"
        );
    }

    @Test
    void outboxLifecycleController_getEndpointsRunningStatus() {
        assertThat(outboxLifecycleController.getEndpointsRunningStatus()).containsOnly(
            Map.entry("auditProducerIncidentsOutbox", true),
            Map.entry("auditProducerOutbox", true),
            Map.entry("commandResultsOutbox", true),
            Map.entry("integrationResultsOutbox", true),
            Map.entry("streamBridgeOutbox", true)
        );
    }

    @Test
    void outboxManager_manageEndpoints() {
        outboxManager.stop();

        await().untilAsserted(() -> assertThat(outboxLifecycleController.allEndpointsRunning()).isFalse());

        outboxManager.start();

        await().untilAsserted(() -> assertThat(outboxLifecycleController.allEndpointsRunning()).isTrue());
    }
}
