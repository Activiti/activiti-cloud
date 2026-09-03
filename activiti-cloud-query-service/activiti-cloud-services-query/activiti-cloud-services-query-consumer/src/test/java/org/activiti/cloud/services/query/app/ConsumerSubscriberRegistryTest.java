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

class ConsumerSubscriberRegistryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration DEAD_AFTER = Duration.ofMinutes(3);

    private ConsumerSubscriberRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ConsumerSubscriberRegistry();
    }

    @Test
    void register_marksUserWatched_andReportsFirstAppearance() {
        boolean firstAppearance = registry.register("alice", List.of("eng"), "rest-1", T0);

        assertThat(firstAppearance).isTrue();
        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.groupsOf("alice")).containsExactlyInAnyOrder("eng");
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-1");
    }

    @Test
    void register_sameUserOnSecondInstance_keepsUser_andIsNotFirstAppearance() {
        registry.register("alice", List.of("eng"), "rest-1", T0);

        boolean firstAppearance = registry.register("alice", List.of("eng"), "rest-2", T0);

        assertThat(firstAppearance).isFalse();
        assertThat(registry.sourcesOf("alice")).containsExactlyInAnyOrder("rest-1", "rest-2");
    }

    @Test
    void unregister_oneOfTwoInstances_keepsUserWatching() {
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.register("alice", List.of("eng"), "rest-2", T0);

        boolean removed = registry.unregister("alice", "rest-1");

        assertThat(removed).isFalse();
        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-2");
    }

    @Test
    void unregister_lastInstance_dropsUser() {
        registry.register("alice", List.of("eng"), "rest-1", T0);

        boolean removed = registry.unregister("alice", "rest-1");

        assertThat(removed).isTrue();
        assertThat(registry.isWatching("alice")).isFalse();
        assertThat(registry.watchedUserIds()).isEmpty();
    }

    @Test
    void unregister_unknownUser_isNoOp() {
        assertThat(registry.unregister("ghost", "rest-1")).isFalse();
    }

    @Test
    void register_refreshesGroupsForExistingUser() {
        registry.register("alice", List.of("eng"), "rest-1", T0);

        registry.register("alice", List.of("eng", "finance"), "rest-2", T0);

        assertThat(registry.groupsOf("alice")).containsExactlyInAnyOrder("eng", "finance");
    }

    @Test
    void heartbeat_keepsInstanceAlive_soItsUsersSurviveExpiry() {
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.heartbeat("rest-1", T0.plus(Duration.ofMinutes(2)));

        Set<String> removed = registry.expireInstances(T0.plus(Duration.ofMinutes(4)), DEAD_AFTER);

        assertThat(removed).isEmpty();
        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void expireInstances_dropsSilentInstance_andRemovesUsersItHeldAlone() {
        registry.register("alice", List.of("eng"), "rest-1", T0);

        Set<String> removed = registry.expireInstances(T0.plus(Duration.ofMinutes(4)), DEAD_AFTER);

        assertThat(removed).containsExactly("alice");
        assertThat(registry.isWatching("alice")).isFalse();
    }

    @Test
    void expireInstances_keepsUserStillHeldByALiveInstance_withReducedSources() {
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.register("alice", List.of("eng"), "rest-2", T0);
        // rest-2 keeps beating; rest-1 goes silent
        registry.heartbeat("rest-2", T0.plus(Duration.ofMinutes(3)));

        Set<String> removed = registry.expireInstances(T0.plus(Duration.ofMinutes(4)), DEAD_AFTER);

        assertThat(removed).isEmpty();
        assertThat(registry.isWatching("alice")).isTrue();
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-2");
    }

    @Test
    void applySnapshot_mergesUsersFromInstance() {
        List<SubscriberRegistryMessage.Entry> entries = List.of(
            new SubscriberRegistryMessage.Entry("alice", List.of("eng")),
            new SubscriberRegistryMessage.Entry("bob", List.of("finance"))
        );

        registry.applySnapshot("rest-9", entries, T0);

        assertThat(registry.watchedUserIds()).containsExactlyInAnyOrder("alice", "bob");
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-9");
        assertThat(registry.groupsOf("bob")).containsExactly("finance");
    }

    @Test
    void snapshotAndNormalRegistration_mergeToTheSameUnion_regardlessOfOrder() {
        SubscriberRegistryMessage.Entry snapshotEntry =
            new SubscriberRegistryMessage.Entry("alice", List.of("eng"));

        // registration first, then a snapshot from another instance
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.applySnapshot("rest-2", List.of(snapshotEntry), T0);
        assertThat(registry.sourcesOf("alice")).containsExactlyInAnyOrder("rest-1", "rest-2");

        // reverse order yields the same union
        ConsumerSubscriberRegistry reversed = new ConsumerSubscriberRegistry();
        reversed.applySnapshot("rest-2", List.of(snapshotEntry), T0);
        reversed.register("alice", List.of("eng"), "rest-1", T0);
        assertThat(reversed.sourcesOf("alice")).containsExactlyInAnyOrder("rest-1", "rest-2");
    }

    @Test
    void groupsAndSourcesOfUnknownUser_areEmpty() {
        assertThat(registry.groupsOf("ghost")).isEmpty();
        assertThat(registry.sourcesOf("ghost")).isEmpty();
        assertThat(registry.isWatching("ghost")).isFalse();
    }

    @Test
    void duplicateRegister_fromSameInstance_isIdempotent() {
        registry.register("alice", List.of("eng"), "rest-1", T0);

        boolean firstAppearanceAgain = registry.register("alice", List.of("eng"), "rest-1", T0);

        assertThat(firstAppearanceAgain).isFalse();
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-1");
    }

    @Test
    void unregisterBeforeRegister_isNoOp_thenRegisterStillWorks() {
        // at-least-once delivery can reorder: an UNREGISTERED for an unknown user must be harmless
        assertThat(registry.unregister("alice", "rest-1")).isFalse();

        boolean firstAppearance = registry.register("alice", List.of("eng"), "rest-1", T0);

        assertThat(firstAppearance).isTrue();
        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void staleUnregister_afterReregisterOnAnotherInstance_onlyRemovesItsOwnSource() {
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.unregister("alice", "rest-1");
        registry.register("alice", List.of("eng"), "rest-2", T0);

        boolean removed = registry.unregister("alice", "rest-1");

        assertThat(removed).isFalse();
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-2");
    }
}
