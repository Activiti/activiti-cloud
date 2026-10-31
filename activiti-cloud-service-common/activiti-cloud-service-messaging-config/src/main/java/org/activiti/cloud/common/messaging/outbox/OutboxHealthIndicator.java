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
package org.activiti.cloud.common.messaging.outbox;

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.SmartLifecycleRoleController;

public class OutboxHealthIndicator implements HealthIndicator {

    private final JdbcChannelMessageStore jdbcChannelMessageStore;
    private final SmartLifecycleRoleController smartLifecycleRoleController;

    // Inject your specific queue channel bean
    public OutboxHealthIndicator(
        JdbcChannelMessageStore jdbcChannelMessageStore,
        SmartLifecycleRoleController smartLifecycleRoleController
    ) {
        this.jdbcChannelMessageStore = jdbcChannelMessageStore;
        this.smartLifecycleRoleController = smartLifecycleRoleController;
    }

    @Override
    public Health health() {
        final var endpoints = smartLifecycleRoleController.getEndpointsRunningStatus(OUTBOX_ROLE);

        final var messageGroups = endpoints
            .keySet()
            .stream()
            .map(key -> Map.entry(key, jdbcChannelMessageStore.messageGroupSize(key)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final var messages = messageGroups.values().stream().mapToInt(Integer::intValue).sum();

        return Health.up()
            .withDetail("running", endpoints.values().stream().allMatch(Boolean.TRUE::equals))
            .withDetail("store", Map.of("messages", messages, "groups", messageGroups))
            .withDetail("pollers", endpoints)
            .build();
    }
}
