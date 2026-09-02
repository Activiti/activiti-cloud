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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SubscriberSessionExpirySweepTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:10:00Z");
    private static final Duration EXPIRY = Duration.ofMinutes(5);

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void should_removeExpiredSession_when_itsLastSeenAtIsPastTheExpiryWindow() {
        SubscriberRegistry registry = new SubscriberRegistry(eventPublisher, 50_000);
        registry.register("alice", Set.of("eng"), "stale-session", NOW.minus(EXPIRY).minusSeconds(1));
        clearInvocations(eventPublisher);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        assertThat(registry.size()).isZero();
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(SubscriberWentQuietEvent.class);
    }

    @Test
    void should_removeExpiredSessionThroughTheSameUnregisterPathAsADisconnect_when_userHasOtherLiveSessionsToo() {
        SubscriberRegistry registry = new SubscriberRegistry(eventPublisher, 50_000);
        registry.register("alice", Set.of("eng"), "stale-session", NOW.minus(EXPIRY).minusSeconds(1));
        registry.register("alice", Set.of("eng"), "fresh-session", NOW);
        clearInvocations(eventPublisher);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        // Only the stale session is gone; the user is still registered because of the fresh one,
        // so no went-quiet transition fires - this is the same "did the map become empty" check
        // a clean disconnect goes through, not a separate code path.
        assertThat(registry.size()).isEqualTo(1);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_leaveALiveSessionUntouched_when_itIsWithinTheExpiryWindow() {
        SubscriberRegistry registry = new SubscriberRegistry(eventPublisher, 50_000);
        registry.register("alice", Set.of("eng"), "session-1", NOW.minusSeconds(30));
        clearInvocations(eventPublisher);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SubscriberSessionExpirySweep sweep = new SubscriberSessionExpirySweep(registry, clock, EXPIRY);

        sweep.sweep();

        assertThat(registry.size()).isEqualTo(1);
        verifyNoInteractions(eventPublisher);
    }
}
