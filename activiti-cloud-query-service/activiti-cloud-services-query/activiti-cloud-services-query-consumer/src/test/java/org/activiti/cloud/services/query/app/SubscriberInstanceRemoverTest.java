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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriberInstanceRemoverTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    @Test
    void removesInstanceSilentPastTimeout_andDropsItsUsers() {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        registry.register("alice", List.of("eng"), "rest-1", T0);
        SubscriberInstanceRemover remover = removerAt(registry, T0.plus(Duration.ofMinutes(4)));

        Set<String> dropped = remover.removeExpiredInstances();

        assertThat(dropped).containsExactly("alice");
        assertThat(registry.isWatching("alice")).isFalse();
    }

    @Test
    void keepsInstanceStillWithinTimeout() {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        registry.register("alice", List.of("eng"), "rest-1", T0);
        SubscriberInstanceRemover remover = removerAt(registry, T0.plus(Duration.ofMinutes(2)));

        Set<String> dropped = remover.removeExpiredInstances();

        assertThat(dropped).isEmpty();
        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void keepsUserStillHeldByALiveInstance() {
        ConsumerSubscriberRegistry registry = new ConsumerSubscriberRegistry();
        registry.register("alice", List.of("eng"), "rest-1", T0);
        registry.register("alice", List.of("eng"), "rest-2", T0);
        registry.heartbeat("rest-2", T0.plus(Duration.ofMinutes(3)));
        SubscriberInstanceRemover remover = removerAt(registry, T0.plus(Duration.ofMinutes(4)));

        Set<String> dropped = remover.removeExpiredInstances();

        assertThat(dropped).isEmpty();
        assertThat(registry.sourcesOf("alice")).containsExactly("rest-2");
    }

    private static SubscriberInstanceRemover removerAt(ConsumerSubscriberRegistry registry, Instant now) {
        return new SubscriberInstanceRemover(registry, TIMEOUT, Clock.fixed(now, ZoneOffset.UTC));
    }
}
