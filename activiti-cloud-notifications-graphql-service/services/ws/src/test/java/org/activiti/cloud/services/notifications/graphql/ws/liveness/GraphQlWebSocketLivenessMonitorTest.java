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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class GraphQlWebSocketLivenessMonitorTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration KEEP_ALIVE = Duration.ofSeconds(5);

    private MutableNanoTime nanoTime;
    private GraphQlWebSocketLivenessMonitor monitor;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        nanoTime = new MutableNanoTime();
        monitor = new GraphQlWebSocketLivenessMonitor(TIMEOUT, Duration.ofSeconds(1), nanoTime, Runnable::run);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void should_closeSessionWithPingTimeoutStatus_when_noMessageArrivesWithinTimeout() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notCloseSession_when_timeoutHasNotElapsedYet() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT.minusMillis(1));

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_neverCloseSession_when_clientKeepsAnsweringPings() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);

        for (int ping = 0; ping < 24; ping++) {
            nanoTime.advance(KEEP_ALIVE);
            monitor.messageReceived(session);
            monitor.messageHandled(session);
            monitor.checkSessions();
        }

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_measureSilenceFromTheEndOfMessageHandling_when_handlingIsSlow() throws Exception {
        monitor.messageReceived(session);
        nanoTime.advance(Duration.ofSeconds(8));
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT.minusSeconds(1));
        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));

        nanoTime.advance(Duration.ofSeconds(1));
        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notCloseSession_when_messageHandlingExceedsTimeout() throws Exception {
        monitor.messageReceived(session);
        nanoTime.advance(TIMEOUT.multipliedBy(2));

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));

        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notCloseSession_when_onlyMessageHandledIsReported() throws Exception {
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT.multipliedBy(3));

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_notStartTrackingSession_when_messageHandledAfterSessionClosed() throws Exception {
        monitor.messageReceived(session);
        monitor.sessionClosed(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_stopTrackingSession_when_sessionClosed() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        monitor.sessionClosed(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();

        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_closeSessionOnlyOnce_when_checkedRepeatedly() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();
        nanoTime.advance(Duration.ofSeconds(1));
        monitor.checkSessions();

        verify(session, times(1)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_dropSessionWithoutClosingIt_when_alreadyClosed() throws Exception {
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        when(session.isOpen()).thenReturn(false);
        nanoTime.advance(TIMEOUT);

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
        monitor.messageHandled(session);
        monitor.messageReceived(activeSession);
        monitor.messageHandled(activeSession);
        nanoTime.advance(TIMEOUT.minusSeconds(1));
        monitor.messageReceived(activeSession);
        monitor.messageHandled(activeSession);
        nanoTime.advance(Duration.ofSeconds(1));

        monitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
        verify(activeSession, never()).close(any(CloseStatus.class));
    }

    @ParameterizedTest
    @MethodSource("closeFailures")
    void should_notPropagateError_when_closingSessionFails(Exception failure) throws Exception {
        doThrow(failure).when(session).close(any(CloseStatus.class));
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        assertThatCode(() -> monitor.checkSessions()).doesNotThrowAnyException();
        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @ParameterizedTest
    @MethodSource("closeFailures")
    void should_retryClosingSession_when_previousCloseFailed(Exception failure) throws Exception {
        doThrow(failure).doNothing().when(session).close(any(CloseStatus.class));
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        monitor.checkSessions();
        nanoTime.advance(Duration.ofSeconds(1));
        monitor.checkSessions();
        nanoTime.advance(Duration.ofSeconds(1));
        monitor.checkSessions();

        verify(session, times(2)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_retryClosingSession_when_closeExecutorRejectedTask() throws Exception {
        AtomicBoolean rejectNext = new AtomicBoolean(true);
        GraphQlWebSocketLivenessMonitor rejectingMonitor = new GraphQlWebSocketLivenessMonitor(
            TIMEOUT,
            Duration.ofSeconds(1),
            nanoTime,
            task -> {
                if (rejectNext.getAndSet(false)) {
                    throw new RejectedExecutionException("queue full");
                }
                task.run();
            }
        );
        rejectingMonitor.messageReceived(session);
        rejectingMonitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);

        assertThatCode(rejectingMonitor::checkSessions).doesNotThrowAnyException();
        verify(session, never()).close(any(CloseStatus.class));

        rejectingMonitor.checkSessions();

        verify(session).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notCloseSession_when_messageArrivesWhileCloseIsQueued() throws Exception {
        List<Runnable> queuedCloses = new ArrayList<>();
        GraphQlWebSocketLivenessMonitor queueingMonitor = new GraphQlWebSocketLivenessMonitor(
            TIMEOUT,
            Duration.ofSeconds(1),
            nanoTime,
            queuedCloses::add
        );
        queueingMonitor.messageReceived(session);
        queueingMonitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);
        queueingMonitor.checkSessions();

        queueingMonitor.messageReceived(session);
        queueingMonitor.messageHandled(session);
        queuedCloses.get(0).run();

        verify(session, never()).close(any(CloseStatus.class));

        nanoTime.advance(TIMEOUT);
        queueingMonitor.checkSessions();
        queuedCloses.get(1).run();

        verify(session, times(1)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_notCloseSession_when_messageIsInProgressWhileCloseRuns() throws Exception {
        List<Runnable> queuedCloses = new ArrayList<>();
        GraphQlWebSocketLivenessMonitor queueingMonitor = new GraphQlWebSocketLivenessMonitor(
            TIMEOUT,
            Duration.ofSeconds(1),
            nanoTime,
            queuedCloses::add
        );
        queueingMonitor.messageReceived(session);
        queueingMonitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);
        queueingMonitor.checkSessions();
        boolean admitted = queueingMonitor.messageReceived(session);
        nanoTime.advance(TIMEOUT);

        queuedCloses.get(0).run();

        assertThat(admitted).isTrue();
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void should_rejectNewMessages_when_closeIsCommitted() throws Exception {
        doThrow(new IOException("broken pipe")).when(session).close(any(CloseStatus.class));
        monitor.messageReceived(session);
        monitor.messageHandled(session);
        nanoTime.advance(TIMEOUT);
        monitor.checkSessions();

        boolean admitted = monitor.messageReceived(session);
        monitor.checkSessions();

        assertThat(admitted).isFalse();
        verify(session, times(2)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_closeRemainingSessionOnNextCheck_when_closeExecutorRejectedTask() throws Exception {
        List<WebSocketSession> closedSessions = new ArrayList<>();
        WebSocketSession otherSession = mock(WebSocketSession.class);
        when(otherSession.getId()).thenReturn("session-2");
        when(otherSession.isOpen()).thenReturn(true);
        recordClosesTo(session, closedSessions);
        recordClosesTo(otherSession, closedSessions);
        AtomicBoolean rejectNext = new AtomicBoolean(true);
        GraphQlWebSocketLivenessMonitor rejectingMonitor = new GraphQlWebSocketLivenessMonitor(
            TIMEOUT,
            Duration.ofSeconds(1),
            nanoTime,
            task -> {
                if (rejectNext.getAndSet(false)) {
                    throw new RejectedExecutionException("queue full");
                }
                task.run();
            }
        );
        rejectingMonitor.messageReceived(session);
        rejectingMonitor.messageHandled(session);
        rejectingMonitor.messageReceived(otherSession);
        rejectingMonitor.messageHandled(otherSession);
        nanoTime.advance(TIMEOUT);

        rejectingMonitor.checkSessions();

        assertThat(closedSessions).hasSize(1);

        rejectingMonitor.checkSessions();

        assertThat(closedSessions).hasSize(2).containsExactlyInAnyOrder(session, otherSession);
        verify(session, times(1)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
        verify(otherSession, times(1)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
    }

    @Test
    void should_closeSilentSessionsPeriodically_when_started() throws Exception {
        GraphQlWebSocketLivenessMonitor runningMonitor = new GraphQlWebSocketLivenessMonitor(
            Duration.ofMillis(100),
            Duration.ofMillis(20),
            System::nanoTime,
            Runnable::run
        );
        runningMonitor.start();
        try {
            runningMonitor.messageReceived(session);
            runningMonitor.messageHandled(session);

            verify(session, timeout(5000)).close(GraphQlWebSocketLivenessMonitor.PING_TIMEOUT_STATUS);
            assertThat(runningMonitor.isRunning()).isTrue();
        } finally {
            runningMonitor.stop();
        }
        assertThat(runningMonitor.isRunning()).isFalse();
    }

    @Test
    void should_stopCheckingSessions_when_stoppedAfterRepeatedStart() throws Exception {
        GraphQlWebSocketLivenessMonitor runningMonitor = new GraphQlWebSocketLivenessMonitor(
            Duration.ofMillis(100),
            Duration.ofMillis(20),
            System::nanoTime,
            Runnable::run
        );
        runningMonitor.start();
        runningMonitor.start();
        try {
            runningMonitor.stop();
            runningMonitor.messageReceived(session);
            runningMonitor.messageHandled(session);

            verify(session, after(500).never()).close(any(CloseStatus.class));
            assertThat(runningMonitor.isRunning()).isFalse();
        } finally {
            runningMonitor.stop();
        }
    }

    static Stream<Arguments> closeFailures() {
        return Stream.of(
            Arguments.of(new IOException("broken pipe")),
            Arguments.of(new IllegalStateException("session already closed"))
        );
    }

    private static void recordClosesTo(
        WebSocketSession recordedSession,
        List<WebSocketSession> closedSessions
    ) throws IOException {
        doAnswer(invocation -> {
            closedSessions.add(recordedSession);
            return null;
        })
            .when(recordedSession)
            .close(any(CloseStatus.class));
    }

    private static final class MutableNanoTime implements LongSupplier {

        // Starts just before overflow, as System.nanoTime() values may wrap around
        private long nanos = Long.MAX_VALUE - Duration.ofSeconds(5).toNanos();

        private void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long getAsLong() {
            return nanos;
        }
    }
}
