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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;

@ExtendWith(MockitoExtension.class)
class OutboxHealthIndicatorTest {

    @Mock
    private JdbcChannelMessageStore jdbcChannelMessageStore;

    @Mock
    private OutboxLifecycleController outboxLifecycleController;

    private OutboxHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new OutboxHealthIndicator(jdbcChannelMessageStore, outboxLifecycleController);
    }

    @Test
    void shouldReportRunningTrueAndAggregateMessageCountsWhenAllEndpointsAreRunning() {
        // Given
        Map<String, Boolean> endpoints = Map.of("ordersOutbox", true, "paymentsOutbox", true);
        when(outboxLifecycleController.getEndpointsRunningStatus()).thenReturn(endpoints);
        when(jdbcChannelMessageStore.messageGroupSize("ordersOutbox")).thenReturn(5);
        when(jdbcChannelMessageStore.messageGroupSize("paymentsOutbox")).thenReturn(3);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
            .containsEntry("running", true)
            .containsEntry("pollers", endpoints)
            .containsEntry("store", Map.of("messages", 8, "groups", Map.of("ordersOutbox", 5, "paymentsOutbox", 3)));

        verify(jdbcChannelMessageStore).messageGroupSize("ordersOutbox");
        verify(jdbcChannelMessageStore).messageGroupSize("paymentsOutbox");
    }

    @Test
    void shouldReportRunningFalseWhenAtLeastOneEndpointIsStopped() {
        // Given
        Map<String, Boolean> endpoints = Map.of("ordersOutbox", true, "paymentsOutbox", false);
        when(outboxLifecycleController.getEndpointsRunningStatus()).thenReturn(endpoints);
        when(jdbcChannelMessageStore.messageGroupSize("ordersOutbox")).thenReturn(10);
        when(jdbcChannelMessageStore.messageGroupSize("paymentsOutbox")).thenReturn(0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
            .containsEntry("running", false)
            .containsEntry("pollers", endpoints)
            .containsEntry("store", Map.of("messages", 10, "groups", Map.of("ordersOutbox", 10, "paymentsOutbox", 0)));
    }

    @Test
    void shouldReportZeroMessagesWhenNoEndpointsExist() {
        // Given
        when(outboxLifecycleController.getEndpointsRunningStatus()).thenReturn(Map.of());

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
            .containsEntry("running", true)
            .containsEntry("pollers", Map.of())
            .containsEntry("store", Map.of("messages", 0, "groups", Map.of()));

        verifyNoInteractions(jdbcChannelMessageStore);
    }
}
