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
package org.activiti.cloud.services.query.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.activiti.cloud.services.query.subscription.ScopeKeys.Badge;
import org.junit.jupiter.api.Test;

class ScopeKeysTest {

    @Test
    void buildsOneKeyShapePerBadge() {
        assertThat(ScopeKeys.assigned("alice")).isEqualTo("assigned:alice");
        assertThat(ScopeKeys.queued("alice")).isEqualTo("queued:alice");
        assertThat(ScopeKeys.processes("alice")).isEqualTo("processes:alice");
    }

    @Test
    void parseRoundTripsBadgeAndUserId() {
        ScopeKeys.ScopeKey parsed = ScopeKeys.parse("queued:alice");

        assertThat(parsed.badge()).isEqualTo(Badge.QUEUED);
        assertThat(parsed.userId()).isEqualTo("alice");
    }

    @Test
    void parsePreservesUserIdContainingSeparator() {
        ScopeKeys.ScopeKey parsed = ScopeKeys.parse("assigned:tenant:alice");

        assertThat(parsed.badge()).isEqualTo(Badge.ASSIGNED);
        assertThat(parsed.userId()).isEqualTo("tenant:alice");
    }

    @Test
    void belongsToMatchesOnlyTheOwningUser() {
        String key = ScopeKeys.processes("alice");

        assertThat(ScopeKeys.belongsTo(key, "alice")).isTrue();
        assertThat(ScopeKeys.belongsTo(key, "bob")).isFalse();
    }

    @Test
    void parseRejectsUnknownPrefix() {
        assertThatThrownBy(() -> ScopeKeys.parse("mentions:alice")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsKeyWithoutSeparator() {
        assertThatThrownBy(() -> ScopeKeys.parse("assigned")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsKeyWithEmptyUserId() {
        assertThatThrownBy(() -> ScopeKeys.parse("assigned:")).isInstanceOf(IllegalArgumentException.class);
    }
}
