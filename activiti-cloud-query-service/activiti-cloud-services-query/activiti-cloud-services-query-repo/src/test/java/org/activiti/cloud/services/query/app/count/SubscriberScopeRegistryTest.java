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
package org.activiti.cloud.services.query.app.count;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriberScopeRegistryTest {

    private SubscriberScopeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SubscriberScopeRegistry(Duration.ofMinutes(5), 100);
    }

    @Test
    void shouldNormalizeAGroupSetSoTheSameMembershipBucketsTogether() {
        registry.record("pluto", List.of("hr", "eng", "hr"));
        registry.record("dave", Arrays.asList("eng", null, "hr"));

        // Equal group sets share a bucket, which is what keeps the query cost off the headcount.
        assertThat(registry.groupsOf("pluto")).containsExactly("eng", "hr").isEqualTo(registry.groupsOf("dave"));
    }

    @Test
    void shouldKeepOneEntryPerUserRatherThanPerGroupSet() {
        registry.record("pluto", List.of("banana"));
        registry.record("dave", List.of("banana"));

        // The old registry kept group sets, so these two collapsed into one entry and neither name survived.
        assertThat(registry.size()).isEqualTo(2);
        assertThat(registry.subscribersHoldingAnyOf(List.of("banana"))).containsExactlyInAnyOrder("pluto", "dave");
    }

    @Test
    void shouldRecordASubscriberWhoHoldsNoGroupsAtAll() {
        registry.record("solo", List.of());
        registry.record("also-solo", null);
        registry.record("nearly-solo", Arrays.asList((String) null));

        // Ignorable while counts were group-scoped; not now - such a user still sees the tasks they are
        // individually named on, plus any task with no candidates at all.
        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.isRegistered("solo")).isTrue();
        assertThat(registry.groupsOf("solo")).isEmpty();
        assertThat(registry.allSubscribers()).contains("solo", "also-solo", "nearly-solo");
    }

    @Test
    void shouldIgnoreASubscriberWithoutAUserId() {
        registry.record(null, List.of("eng"));
        registry.record("  ", List.of("eng"));

        // A per-user count with no user to send it to is not a count.
        assertThat(registry.size()).isZero();
    }

    @Test
    void shouldReturnSubscribersWhoseGroupSetIntersectsRatherThanOnlyThoseContained() {
        registry.record("pluto", List.of("eng"));
        registry.record("dave", List.of("eng", "hr"));
        registry.record("pippo", List.of("finance"));

        // A user in {eng, hr} can see tasks offered to eng alone, so touching eng moves their count too.
        assertThat(registry.subscribersHoldingAnyOf(List.of("eng"))).containsExactlyInAnyOrder("pluto", "dave");
    }

    @Test
    void shouldReturnNobodyWhenNoSubscriberHoldsAnAffectedGroup() {
        registry.record("pluto", List.of("eng"));

        assertThat(registry.subscribersHoldingAnyOf(List.of("legal"))).isEmpty();
        assertThat(registry.subscribersHoldingAnyOf(List.of())).isEmpty();
        assertThat(registry.subscribersHoldingAnyOf(null)).isEmpty();
    }

    @Test
    void shouldMatchOnAnyOfTheAffectedGroups() {
        registry.record("dave", List.of("eng", "hr"));
        registry.record("pippo", List.of("finance"));

        assertThat(registry.subscribersHoldingAnyOf(Set.of("legal", "finance"))).containsExactly("pippo");
    }

    @Test
    void shouldForgetASubscriberOnDeregistration() {
        registry.record("pluto", List.of("eng"));

        registry.deregister("pluto");
        registry.deregister(null);

        // The websocket closing is the only signal that actually means "stopped watching".
        assertThat(registry.isRegistered("pluto")).isFalse();
        assertThat(registry.groupsOf("pluto")).isEmpty();
        assertThat(registry.subscribersHoldingAnyOf(List.of("eng"))).isEmpty();
    }

    @Test
    void shouldDropSubscribersOnceTheExpiryHasElapsed() {
        SubscriberScopeRegistry shortLived = new SubscriberScopeRegistry(Duration.ZERO, 100);
        shortLived.record("pluto", List.of("eng"));

        // Until registration follows the socket's lifetime, this expiry is still a correctness parameter: a
        // subscriber that stops re-fetching falls out and silently stops receiving pushes.
        assertThat(shortLived.subscribersHoldingAnyOf(List.of("eng"))).isEmpty();
        assertThat(shortLived.isRegistered("pluto")).isFalse();
    }

    @Test
    void shouldReportAnUnknownUserAsNeitherRegisteredNorGrouped() {
        assertThat(registry.isRegistered("nobody")).isFalse();
        assertThat(registry.isRegistered(null)).isFalse();
        assertThat(registry.groupsOf("nobody")).isEmpty();
        assertThat(registry.groupsOf(null)).isEmpty();
    }
}
