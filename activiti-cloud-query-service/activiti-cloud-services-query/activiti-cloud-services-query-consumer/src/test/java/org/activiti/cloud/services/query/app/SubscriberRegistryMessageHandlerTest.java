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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriberRegistryMessageHandlerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private ConsumerSubscriberRegistry registry;
    private SubscriberRegistryMessageHandler handler;

    @BeforeEach
    void setUp() {
        registry = new ConsumerSubscriberRegistry();
        handler = new SubscriberRegistryMessageHandler(registry);
    }

    @Test
    void registeredMessage_addsWatchingUserWithGroups() {
        handler.handle(SubscriberRegistryMessage.registered("alice", List.of("eng"), "rest-1", T0));

        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.groupsOf("alice")).containsExactly("eng");
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-1");
    }

    @Test
    void unregisteredMessage_removesUserHeldByOnlyThatInstance() {
        handler.handle(SubscriberRegistryMessage.registered("alice", List.of("eng"), "rest-1", T0));

        handler.handle(SubscriberRegistryMessage.unregistered("alice", "rest-1", T0));

        assertThat(registry.isWatching("alice")).isFalse();
    }

    @Test
    void heartbeatMessage_keepsInstanceAlive() {
        handler.handle(SubscriberRegistryMessage.registered("alice", List.of("eng"), "rest-1", T0));
        handler.handle(SubscriberRegistryMessage.heartbeat("rest-1", T0.plus(Duration.ofMinutes(2))));

        Set<String> removed = registry.expireInstances(T0.plus(Duration.ofMinutes(4)), Duration.ofMinutes(3));

        assertThat(removed).isEmpty();
        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void snapshotMessage_mergesEntries() {
        SubscriberRegistryMessage snapshot = SubscriberRegistryMessage.snapshot(
            List.of(
                new SubscriberRegistryMessage.Entry("alice", List.of("eng")),
                new SubscriberRegistryMessage.Entry("bob", List.of("finance"))
            ),
            "rest-9",
            T0
        );

        handler.handle(snapshot);

        assertThat(registry.watchedUserIds()).containsExactlyInAnyOrder("alice", "bob");
        assertThat(registry.sourcesOf("bob")).containsExactly("rest-9");
    }

    @Test
    void resyncRequestMessage_isIgnored() {
        handler.handle(SubscriberRegistryMessage.resyncRequest("consumer-1", T0));

        assertThat(registry.watchedUserIds()).isEmpty();
        assertThat(registry.size()).isZero();
    }
}
