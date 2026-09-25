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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriberSessionExpirySweepTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:10:00Z");
    private static final Duration EXPIRY = Duration.ofMinutes(5);

    @Test
    void should_removeExpiredSession_when_itsLastSeenAtIsPastTheExpiryWindow() {
        SubscriberRegistry registry = new SubscriberRegistry(50_000);
        registry.register("alice", Set.of("eng"), "stale-session", NOW.minus(EXPIRY).minusSeconds(1));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        assertThat(registry.size()).isZero();
        assertThat(registry.isWatching("alice")).isFalse();
    }

    @Test
    void should_removeExpiredSessionThroughTheSameUnregisterPathAsADisconnect_when_userHasOtherLiveSessionsToo() {
        SubscriberRegistry registry = new SubscriberRegistry(50_000);
        registry.register("alice", Set.of("eng"), "stale-session", NOW.minus(EXPIRY).minusSeconds(1));
        registry.register("alice", Set.of("eng"), "fresh-session", NOW);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void should_leaveALiveSessionUntouched_when_itIsWithinTheExpiryWindow() {
        SubscriberRegistry registry = new SubscriberRegistry(50_000);
        registry.register("alice", Set.of("eng"), "session-1", NOW.minusSeconds(30));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.isWatching("alice")).isTrue();
    }
}
