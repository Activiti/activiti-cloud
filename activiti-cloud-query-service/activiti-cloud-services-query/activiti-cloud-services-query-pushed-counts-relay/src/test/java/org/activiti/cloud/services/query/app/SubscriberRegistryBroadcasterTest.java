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
package org.activiti.cloud.services.query.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.activiti.cloud.services.query.subscription.SubscriberRegistrySnapshot;
import org.activiti.cloud.services.query.subscription.SubscriberWentLiveEvent;
import org.activiti.cloud.services.query.subscription.SubscriberWentQuietEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageChannel;

class SubscriberRegistryBroadcasterTest {

    private static final String SOURCE_ID = "instance-a";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final List<SubscriberRegistryMessage> sent = new ArrayList<>();
    private List<SubscriberRegistryMessage.Entry> registrySnapshot = List.of();
    private SubscriberRegistryBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        MessageChannel channel = (message, timeout) -> {
            sent.add((SubscriberRegistryMessage) message.getPayload());
            return true;
        };
        SubscriberRegistrySnapshot registry = () -> registrySnapshot;
        broadcaster = new SubscriberRegistryBroadcaster(channel, registry, SOURCE_ID, CLOCK);
    }

    @Test
    void broadcastsRegistered_onWentLive() {
        broadcaster.onWentLive(new SubscriberWentLiveEvent("alice", Set.of("dev", "qa"), NOW));

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage message = sent.getFirst();
        assertThat(message.type()).isEqualTo(RegistryMessageType.REGISTERED);
        assertThat(message.userId()).isEqualTo("alice");
        assertThat(message.groups()).containsExactlyInAnyOrder("dev", "qa");
        assertThat(message.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(message.sentAt()).isEqualTo(NOW);
    }

    @Test
    void broadcastsRegisteredWithEmptyGroups_whenWentLiveHasNullGroups() {
        broadcaster.onWentLive(new SubscriberWentLiveEvent("alice", null, NOW));

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage message = sent.getFirst();
        assertThat(message.type()).isEqualTo(RegistryMessageType.REGISTERED);
        assertThat(message.userId()).isEqualTo("alice");
        assertThat(message.groups()).isEmpty();
    }

    @Test
    void broadcastsUnregistered_onWentQuiet() {
        broadcaster.onWentQuiet(new SubscriberWentQuietEvent("alice", NOW));

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage message = sent.getFirst();
        assertThat(message.type()).isEqualTo(RegistryMessageType.UNREGISTERED);
        assertThat(message.userId()).isEqualTo("alice");
        assertThat(message.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(message.sentAt()).isEqualTo(NOW);
    }

    @Test
    void broadcastsSnapshotOfWholeRegistry_onStartup() {
        registrySnapshot = List.of(
            new SubscriberRegistryMessage.Entry("alice", List.of("dev")),
            new SubscriberRegistryMessage.Entry("bob", List.of())
        );

        broadcaster.onStartup();

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage message = sent.getFirst();
        assertThat(message.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(message.entries()).hasSize(2);
        assertThat(message.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(message.sentAt()).isEqualTo(NOW);
    }

    @Test
    void broadcastsHeartbeat() {
        broadcaster.heartbeat();

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage message = sent.getFirst();
        assertThat(message.type()).isEqualTo(RegistryMessageType.HEARTBEAT);
        assertThat(message.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(message.sentAt()).isEqualTo(NOW);
    }

    @Test
    void swallowsSendFailure_soABrokerHiccupNeverBreaksTheSubscribeFlow() {
        MessageChannel failing = (message, timeout) -> {
            throw new RuntimeException("broker down");
        };
        SubscriberRegistryBroadcaster resilient = new SubscriberRegistryBroadcaster(
            failing,
            () -> registrySnapshot,
            SOURCE_ID,
            CLOCK
        );

        assertThatCode(() ->
            resilient.onWentLive(new SubscriberWentLiveEvent("alice", Set.of("dev"), NOW))
        ).doesNotThrowAnyException();
    }
}
