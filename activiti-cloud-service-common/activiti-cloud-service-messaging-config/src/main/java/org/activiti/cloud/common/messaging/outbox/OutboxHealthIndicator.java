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

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.scheduling.annotation.Scheduled;

public class OutboxHealthIndicator implements HealthIndicator {

    private final JdbcChannelMessageStore jdbcChannelMessageStore;
    private final OutboxLifecycleController outboxLifecycleController;

    private Map<String, Object> storeDetails = Map.of();

    // Inject your specific queue channel bean
    public OutboxHealthIndicator(
        JdbcChannelMessageStore jdbcChannelMessageStore,
        OutboxLifecycleController outboxLifecycleController
    ) {
        this.jdbcChannelMessageStore = jdbcChannelMessageStore;
        this.outboxLifecycleController = outboxLifecycleController;
    }

    @Scheduled(fixedDelayString = "${activiti.cloud.messaging.outbox.health.fixed-delay:1m}")
    void refreshStoreDetails() {
        this.storeDetails = Map.copyOf(storeDetails());
    }

    @Override
    public Health health() {
        final var endpoints = outboxLifecycleController.getEndpointsRunningStatus();

        return Health.up()
            .withDetail("running", endpoints.values().stream().allMatch(Boolean.TRUE::equals))
            .withDetail(
                "store",
                Optional.of(storeDetails).filter(Predicate.not(Map::isEmpty)).orElseGet(this::storeDetails)
            )
            .withDetail("pollers", endpoints)
            .build();
    }

    private Map<String, Object> storeDetails() {
        final var messageGroups = outboxLifecycleController
            .getEndpointsRunningStatus()
            .keySet()
            .stream()
            .map(key -> Map.entry(key, jdbcChannelMessageStore.messageGroupSize(key)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final var messages = messageGroups.values().stream().mapToInt(Integer::intValue).sum();

        return Map.of("messages", messages, "groups", messageGroups);
    }
}
