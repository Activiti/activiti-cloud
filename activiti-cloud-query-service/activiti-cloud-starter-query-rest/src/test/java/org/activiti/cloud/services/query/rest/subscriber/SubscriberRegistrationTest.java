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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriberRegistrationTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final SubscriberRegistration registration = new SubscriberRegistration("alice", Set.of("eng", "finance"));

    @Test
    void should_reportZeroToOneTransition_when_firstSessionIsAdded() {
        boolean wentLive = registration.addSession("session-1", now);

        assertThat(wentLive).isTrue();
        assertThat(registration.isEmpty()).isFalse();
        assertThat(registration.sessionCount()).isEqualTo(1);
    }

    @Test
    void should_notReportTransition_when_secondSessionIsAddedForAnAlreadyLiveUser() {
        registration.addSession("session-1", now);

        boolean wentLive = registration.addSession("session-2", now);

        assertThat(wentLive).isFalse();
        assertThat(registration.sessionCount()).isEqualTo(2);
    }

    @Test
    void should_reportOneToZeroTransition_when_theLastSessionIsRemoved() {
        registration.addSession("session-1", now);

        boolean wentQuiet = registration.removeSession("session-1");

        assertThat(wentQuiet).isTrue();
        assertThat(registration.isEmpty()).isTrue();
    }

    @Test
    void should_notReportTransition_when_oneOfTwoSessionsIsRemoved() {
        registration.addSession("session-1", now);
        registration.addSession("session-2", now);

        boolean wentQuiet = registration.removeSession("session-1");

        assertThat(wentQuiet).isFalse();
        assertThat(registration.isEmpty()).isFalse();
        assertThat(registration.sessionCount()).isEqualTo(1);
    }

    @Test
    void should_beANoOp_when_touchingAnUnknownSessionId() {
        registration.addSession("session-1", now);

        registration.touch("session-does-not-exist", now.plusSeconds(60));

        assertThat(registration.expiredSessionIds(now.plusSeconds(30), Duration.ofSeconds(10))).containsExactly(
            "session-1"
        );
    }

    @Test
    void should_refreshLastSeenAt_when_touchingAKnownSession() {
        registration.addSession("session-1", now);

        registration.touch("session-1", now.plusSeconds(100));

        assertThat(registration.expiredSessionIds(now.plusSeconds(110), Duration.ofSeconds(30))).isEmpty();
    }

    @Test
    void should_returnOnlySessionsPastTheExpiryWindow_when_someSessionsAreStillFresh() {
        registration.addSession("stale-session", now);
        registration.addSession("fresh-session", now);
        registration.touch("fresh-session", now.plusSeconds(50));

        Set<String> expired = registration.expiredSessionIds(now.plusSeconds(60), Duration.ofSeconds(30));

        assertThat(expired).containsExactly("stale-session");
    }

    @Test
    void should_snapshotGroups_when_constructed() {
        assertThat(registration.getGroups()).containsExactlyInAnyOrder("eng", "finance");
    }
}
