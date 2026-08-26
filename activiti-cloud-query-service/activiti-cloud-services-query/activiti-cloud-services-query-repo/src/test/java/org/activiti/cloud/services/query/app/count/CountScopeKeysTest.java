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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CountScopeKeysTest {

    @Test
    void shouldNormalizeGroupsToNullFreeDistinctSortedOrder() {
        assertThat(CountScopeKeys.normalizeGroups(Arrays.asList("hr", null, "eng", "hr"))).containsExactly("eng", "hr");
        assertThat(CountScopeKeys.normalizeGroups(null)).isEmpty();
        assertThat(CountScopeKeys.normalizeGroups(List.of())).isEmpty();
    }

    @Test
    void shouldBuildTheSameGroupKeyForTheSameMembershipInAnyOrder() {
        assertThat(CountScopeKeys.forGroups(List.of("hr", "eng"))).isEqualTo("groups:eng,hr");
        assertThat(CountScopeKeys.forGroups(List.of("eng", "hr"))).isEqualTo(
            CountScopeKeys.forGroups(List.of("hr", "eng"))
        );
    }

    @Test
    void shouldRejectAGroupKeyWithoutGroups() {
        // An empty group set would name an unrestricted count, which is not what a group scope means.
        assertThatThrownBy(() -> CountScopeKeys.forGroups(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one group");
        assertThatThrownBy(() -> CountScopeKeys.forGroups(Arrays.asList((String) null))).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectAGroupIdContainingTheSeparator() {
        assertThatThrownBy(() -> CountScopeKeys.forGroups(List.of("eng,hr")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ambiguous");
    }

    @Test
    void shouldBuildUserScopedKeys() {
        assertThat(CountScopeKeys.forUser("alice")).isEqualTo("user:alice");
        assertThat(CountScopeKeys.forProcessUser("alice")).isEqualTo("puser:alice");
    }

    @Test
    void shouldRecogniseOnlyGroupKeysAsGroupScopes() {
        assertThat(CountScopeKeys.isGroupScope(CountScopeKeys.forGroups(List.of("eng")))).isTrue();
        assertThat(CountScopeKeys.isGroupScope(CountScopeKeys.forUser("alice"))).isFalse();
        assertThat(CountScopeKeys.isGroupScope(CountScopeKeys.forProcessUser("alice"))).isFalse();
        assertThat(CountScopeKeys.isGroupScope(null)).isFalse();
    }

    @Test
    void shouldRoundTripAGroupKeyBackToItsGroups() {
        List<String> groups = List.of("eng", "hr");

        assertThat(CountScopeKeys.groupsOf(CountScopeKeys.forGroups(groups))).isEqualTo(groups);
    }

    @Test
    void shouldRefuseToParseANonGroupKey() {
        assertThatThrownBy(() -> CountScopeKeys.groupsOf(CountScopeKeys.forUser("alice")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a group scope key");
    }
}
