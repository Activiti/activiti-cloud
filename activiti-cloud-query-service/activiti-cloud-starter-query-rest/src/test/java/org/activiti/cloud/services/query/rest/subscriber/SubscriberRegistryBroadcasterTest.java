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
package org.activiti.cloud.services.query.rest.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;

class SubscriberRegistryBroadcasterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String DESTINATION = "subscriberRegistry";
    private static final String SOURCE_ID = "rest-1";

    private StreamBridge streamBridge;
    private SubscriberRegistry registry;
    private SubscriberRegistryBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        streamBridge = mock(StreamBridge.class);
        registry = new SubscriberRegistry(mock(ApplicationEventPublisher.class), 50_000);
        broadcaster =
            new SubscriberRegistryBroadcaster(
                streamBridge,
                registry,
                SOURCE_ID,
                DESTINATION,
                Clock.fixed(NOW, ZoneOffset.UTC)
            );
    }

    @Test
    void should_broadcastRegisteredWithGroupsAndSourceId_when_aUserGoesLive() {
        broadcaster.onWentLive(new SubscriberWentLiveEvent("alice", Set.of("eng"), NOW));

        SubscriberRegistryMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(RegistryMessageType.REGISTERED);
        assertThat(sent.userId()).isEqualTo("alice");
        assertThat(sent.groups()).containsExactly("eng");
        assertThat(sent.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(sent.sentAt()).isEqualTo(NOW);
    }

    @Test
    void should_broadcastUnregistered_when_aUserGoesQuiet() {
        broadcaster.onWentQuiet(new SubscriberWentQuietEvent("alice", NOW));

        SubscriberRegistryMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(RegistryMessageType.UNREGISTERED);
        assertThat(sent.userId()).isEqualTo("alice");
        assertThat(sent.sourceId()).isEqualTo(SOURCE_ID);
    }

    @Test
    void should_broadcastHeartbeatStampedWithTheClock_when_heartbeatFires() {
        broadcaster.heartbeat();

        SubscriberRegistryMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(RegistryMessageType.HEARTBEAT);
        assertThat(sent.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(sent.sentAt()).isEqualTo(NOW);
    }

    @Test
    void should_broadcastASnapshotOfTheWholeRegistry_when_theInstanceStarts() {
        registry.register("alice", Set.of("eng"), "session-1", NOW);
        registry.register("bob", Set.of("sales", "ops"), "session-2", NOW);

        broadcaster.onStartup();

        SubscriberRegistryMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(sent.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(sent.entries())
            .extracting(SubscriberRegistryMessage.Entry::userId)
            .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void should_broadcastAnEmptySnapshot_when_theInstanceStartsWithNoSubscribers() {
        broadcaster.onStartup();

        SubscriberRegistryMessage sent = captureSent();
        assertThat(sent.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(sent.entries()).isEmpty();
    }

    private SubscriberRegistryMessage captureSent() {
        ArgumentCaptor<SubscriberRegistryMessage> captor = ArgumentCaptor.forClass(SubscriberRegistryMessage.class);
        verify(streamBridge).send(eq(DESTINATION), captor.capture());
        return captor.getValue();
    }
}
