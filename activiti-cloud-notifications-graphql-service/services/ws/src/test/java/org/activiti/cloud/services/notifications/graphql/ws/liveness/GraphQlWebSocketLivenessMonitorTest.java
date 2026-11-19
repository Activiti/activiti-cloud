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
package org.activiti.cloud.services.notifications.graphql.ws.liveness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class GraphQlWebSocketLivenessMonitorTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration KEEP_ALIVE = Duration.ofSeconds(5);

    private MutableClock clock;
    private GraphQlWebSocketLivenessMonitor monitor;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        monitor = new GraphQlWebSocketLivenessMonitor(TIMEOUT, Duration.ofSeconds(1), clock, Runnable::run);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void should_closeSessionWithPingTimeoutStatus_when_noMessageArrivesWithinTimeout() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        clock.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_useDedicatedPingTimeoutCloseCode() {
        assertThat(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS.getCode()).isEqualTo(4410);
        assertThat(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS.getReason()).isEqualTo("Ping timeout");
    }

    @Test
    void should_notCloseSession_when_timeoutHasNotElapsedYet() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        clock.advance(TIMEOUT.minusMillis(1));

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_neverCloseSession_when_clientKeepsAnsweringPings() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);

        for (int ping = 0; ping < 24; ping++) {
            clock.advance(KEEP_ALIVE);
            monitor.messageReceived(session);
            monitor.messageHandled(session);
            monitor.checkSessions();
        }

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_measureSilenceFromTheEndOfMessageHandling_when_handlingIsSlow() throws Exception {
        monitor.messageReceived(session);
        clock.advance(Duration.ofSeconds(8));
        monitor.messageHandled(session);
        clock.advance(TIMEOUT.minusSeconds(1));
        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));

        clock.advance(Duration.ofSeconds(1));
        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notTrackSession_when_noMessageWasReceived() throws Exception {
        clock.advance(TIMEOUT.multipliedBy(3));

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_notStartTrackingSession_when_messageHandledAfterSessionClosed() throws Exception {
        monitor.messageReceived(session);
        monitor.sessionClosed(session);
        monitor.messageHandled(session);
        clock.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_stopTrackingSession_when_sessionClosed() throws Exception {
        monitor.messageReceived(session);
        monitor.sessionClosed(session);
        clock.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_closeSessionOnlyOnce_when_checkedRepeatedly() throws Exception {
        monitor.messageReceived(session);
        clock.advance(TIMEOUT);

        monitor.checkSessions();
        clock.advance(Duration.ofSeconds(1));
        monitor.checkSessions();

        verify(session, times(1)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_dropSessionWithoutClosingIt_when_alreadyClosed() throws Exception {
        monitor.messageReceived(session);
        when(session.isOpen()).thenReturn(false);
        clock.advance(TIMEOUT);

        monitor.checkSessions();
        when(session.isOpen()).thenReturn(true);
        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_onlyCloseSilentSessions_when_severalSessionsAreTracked() throws Exception {
        WebSocketSession activeSession = mock(WebSocketSession.class);
        when(activeSession.getId()).thenReturn("session-2");
        when(activeSession.isOpen()).thenReturn(true);
        monitor.messageReceived(session);
        monitor.messageReceived(activeSession);
        clock.advance(TIMEOUT.minusSeconds(1));
        monitor.messageReceived(activeSession);
        clock.advance(Duration.ofSeconds(1));

        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
        verify(activeSession, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_notPropagateError_when_closingSessionFails() throws Exception {
        doThrow(new IOException("broken pipe")).when(session).close(any(CloseStatus.class));
        monitor.messageReceived(session);
        clock.advance(TIMEOUT);

        assertThatCode(() -> monitor.checkSessions()).doesNotThrowAnyException();
        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_closeSilentSessionsPeriodically_when_started() throws Exception {
        GraphQlWebSocketLivenessMonitor runningMonitor = new GraphQlWebSocketLivenessMonitor(
            Duration.ofMillis(100),
            Duration.ofMillis(20),
            Clock.systemUTC(),
            Runnable::run
        );
        runningMonitor.start();
        try {
            runningMonitor.messageReceived(session);

            verify(session, timeout(5000)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
            assertThat(runningMonitor.isRunning()).isTrue();
        } finally {
            runningMonitor.stop();
        }
        assertThat(runningMonitor.isRunning()).isFalse();
    }

    @Test
    void should_rejectNonPositiveDurations() {
        assertThatThrownBy(() ->
            new GraphQlWebSocketLivenessMonitor(Duration.ZERO, Duration.ofSeconds(1), clock, Runnable::run)
        ).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
            new GraphQlWebSocketLivenessMonitor(TIMEOUT, Duration.ZERO, clock, Runnable::run)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
