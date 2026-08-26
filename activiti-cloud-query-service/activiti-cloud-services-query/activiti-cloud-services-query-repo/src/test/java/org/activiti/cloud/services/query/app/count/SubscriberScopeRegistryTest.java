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
    void shouldTreatTheSameMembershipInAnyOrderAsOneScope() {
        registry.record(List.of("eng", "hr"));
        registry.record(List.of("hr", "eng"));

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.groupSetsIntersecting(List.of("eng"))).containsExactly(List.of("eng", "hr"));
    }

    @Test
    void shouldIgnoreGroupSetsThatCouldNotHaveAGroupScopedCount() {
        registry.record(List.of());
        registry.record(null);
        registry.record(Arrays.asList((String) null));

        assertThat(registry.size()).isZero();
        assertThat(registry.groupSetsIntersecting(List.of("eng"))).isEmpty();
    }

    @Test
    void shouldReturnGroupSetsThatIntersectRatherThanOnlyThoseContained() {
        registry.record(List.of("eng"));
        registry.record(List.of("eng", "hr"));
        registry.record(List.of("finance"));

        // A user in {eng, hr} can see tasks offered to eng alone, so touching eng changes their count too.
        assertThat(registry.groupSetsIntersecting(List.of("eng"))).containsExactlyInAnyOrder(
            List.of("eng"),
            List.of("eng", "hr")
        );
    }

    @Test
    void shouldReturnNothingWhenNoRecordedGroupSetIsAffected() {
        registry.record(List.of("eng"));

        assertThat(registry.groupSetsIntersecting(List.of("legal"))).isEmpty();
        assertThat(registry.groupSetsIntersecting(List.of())).isEmpty();
        assertThat(registry.groupSetsIntersecting(null)).isEmpty();
    }

    @Test
    void shouldMatchOnAnyOfTheAffectedGroups() {
        registry.record(List.of("eng", "hr"));
        registry.record(List.of("finance"));

        assertThat(registry.groupSetsIntersecting(Set.of("legal", "finance"))).containsExactly(List.of("finance"));
    }

    @Test
    void shouldDropRecordedGroupSetsOnceTheTtlHasElapsed() {
        SubscriberScopeRegistry shortLived = new SubscriberScopeRegistry(Duration.ZERO, 100);
        shortLived.record(List.of("eng"));

        // A subscriber that stops re-fetching falls out of the registry and stops receiving pushes.
        assertThat(shortLived.groupSetsIntersecting(List.of("eng"))).isEmpty();
    }
}
