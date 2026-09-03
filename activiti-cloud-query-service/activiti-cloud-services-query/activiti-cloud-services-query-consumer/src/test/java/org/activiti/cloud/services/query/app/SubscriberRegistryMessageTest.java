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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.Test;

class SubscriberRegistryMessageTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void registeredCarriesUserAndGroups_butNoEntries() {
        SubscriberRegistryMessage message = SubscriberRegistryMessage.registered(
            "alice",
            List.of("eng"),
            "rest-1",
            NOW
        );

        assertThat(message.type()).isEqualTo(RegistryMessageType.REGISTERED);
        assertThat(message.userId()).isEqualTo("alice");
        assertThat(message.groups()).containsExactly("eng");
        assertThat(message.entries()).isNull();
        assertThat(message.sourceId()).isEqualTo("rest-1");
    }

    @Test
    void unregisteredCarriesUser_butNoGroupsOrEntries() {
        SubscriberRegistryMessage message = SubscriberRegistryMessage.unregistered("alice", "rest-1", NOW);

        assertThat(message.type()).isEqualTo(RegistryMessageType.UNREGISTERED);
        assertThat(message.userId()).isEqualTo("alice");
        assertThat(message.groups()).isNull();
        assertThat(message.entries()).isNull();
    }

    @Test
    void heartbeatCarriesOnlySource() {
        SubscriberRegistryMessage message = SubscriberRegistryMessage.heartbeat("rest-1", NOW);

        assertThat(message.type()).isEqualTo(RegistryMessageType.HEARTBEAT);
        assertThat(message.userId()).isNull();
        assertThat(message.groups()).isNull();
        assertThat(message.entries()).isNull();
        assertThat(message.sourceId()).isEqualTo("rest-1");
    }

    @Test
    void snapshotCarriesEntries() {
        SubscriberRegistryMessage message = SubscriberRegistryMessage.snapshot(
            List.of(new SubscriberRegistryMessage.Entry("alice", List.of("eng"))),
            "rest-1",
            NOW
        );

        assertThat(message.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(message.entries()).hasSize(1);
        assertThat(message.entries().get(0).userId()).isEqualTo("alice");
        assertThat(message.entries().get(0).groups()).containsExactly("eng");
    }

    @Test
    void requiredFieldsAreValidated() {
        assertThatThrownBy(() -> SubscriberRegistryMessage.heartbeat(null, NOW)).isInstanceOf(
            NullPointerException.class
        );
        assertThatThrownBy(() -> SubscriberRegistryMessage.registered("alice", List.of(), "rest-1", null)).isInstanceOf(
            NullPointerException.class
        );
    }

    @Test
    void groupsListIsDefensivelyCopied() {
        List<String> mutableGroups = new ArrayList<>(List.of("eng"));

        SubscriberRegistryMessage message = SubscriberRegistryMessage.registered("alice", mutableGroups, "rest-1", NOW);
        mutableGroups.add("finance");

        assertThat(message.groups()).containsExactly("eng");
    }
}
