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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.config.RabbitMqDrainHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Integration tests for {@code activiti.cloud.messaging.migration-mode=true}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>The migration-mode flag is read correctly.
 *   <li>All {@link org.activiti.cloud.common.messaging.functional.OutputBinding @OutputBinding}
 *       channels silently drop messages (send returns {@code false}).
 *   <li>The {@link RabbitMqDrainHealthIndicator} is registered and reports correct health.
 * </ul>
 */
@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "activiti.cloud.messaging.migration-mode=true",
        "activiti.cloud.messaging.destination-transformers-enabled=false",
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.group=bar",
        "spring.cloud.stream.bindings.auditProducer.destination=engineEvents",
        "spring.cloud.stream.bindings.auditConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.queryConsumer.destination=engineEvents",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
    }
)
@Import({ TestChannelBinderConfiguration.class, TestBindingsChannelsConfiguration.class })
public class MigrationModeMessagingEnabledIT {

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private TestBindingsChannels channels;

    @Autowired(required = false)
    private RabbitMqDrainHealthIndicator drainHealthIndicator;

    @MockBean
    private RabbitAdmin rabbitAdmin;

    @Test
    void migrationModePropertyIsTrue() {
        assertThat(messagingProperties.isMigrationMode()).isTrue();
    }

    @Test
    void outputChannelDropsMessagesInMigrationMode() {
        MessageChannel commandResults = channels.commandResults();

        // The DroppingChannelInterceptor returns null from preSend which causes send() to return false
        boolean sent = commandResults.send(MessageBuilder.withPayload("payload").build(), 100);

        assertThat(sent).isFalse();
    }

    @Test
    void allOutputChannelsDropMessages() {
        assertThat(channels.auditProducer().send(MessageBuilder.withPayload("a").build(), 100)).isFalse();
        assertThat(channels.auditProducerIncidents().send(MessageBuilder.withPayload("b").build(), 100)).isFalse();
        assertThat(channels.integrationResults().send(MessageBuilder.withPayload("c").build(), 100)).isFalse();
    }

    @Test
    void drainHealthIndicatorIsRegistered() {
        assertThat(drainHealthIndicator).isNotNull();
    }

    @Test
    void drainHealthIndicatorReportsUpWhenQueuesAreEmpty() {
        // given: RabbitAdmin returns empty properties (message count defaults to 0)
        when(rabbitAdmin.getQueueProperties(anyString())).thenReturn(new Properties());

        // when
        Health health = drainHealthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry(RabbitMqDrainHealthIndicator.MIGRATION_MODE_KEY, RabbitMqDrainHealthIndicator.DRAIN_COMPLETE);
    }

    @Test
    void drainHealthIndicatorReportsOutOfServiceWhenQueuesHavePendingMessages() {
        // given: queue has 5 pending messages
        Properties queueProperties = new Properties();
        queueProperties.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 5L);
        when(rabbitAdmin.getQueueProperties(anyString())).thenReturn(queueProperties);

        // when
        Health health = drainHealthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry(RabbitMqDrainHealthIndicator.MIGRATION_MODE_KEY, RabbitMqDrainHealthIndicator.DRAINING);
        assertThat(health.getDetails()).containsKey(RabbitMqDrainHealthIndicator.TOTAL_PENDING_MESSAGES_KEY);
        assertThat((Long) health.getDetails().get(RabbitMqDrainHealthIndicator.TOTAL_PENDING_MESSAGES_KEY)).isGreaterThan(0);
    }

    @Test
    void drainHealthIndicatorReportsUpWhenRabbitAdminReturnsNull() {
        // given: queue not found (admin returns null)
        when(rabbitAdmin.getQueueProperties(anyString())).thenReturn(null);

        // when
        Health health = drainHealthIndicator.health();

        // then: treats missing queue as 0 messages (safe to proceed)
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
}
