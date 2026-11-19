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

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Closes GraphQL WebSocket sessions that received no message from the client within the timeout. Any inbound
 * message counts as proof of liveness, in particular the pong a client sends back for every keep-alive ping.
 * A session is tracked from its first inbound message on, until then Spring's connection initialisation
 * timeout applies.
 */
public class GraphQlWebSocketLivenessMonitor implements SmartLifecycle {

    public static final CloseStatus PING_TIMEOUT_STATUS = new CloseStatus(4410, "Ping timeout");

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlWebSocketLivenessMonitor.class);

    private final Duration timeout;
    private final Duration checkInterval;
    private final Clock clock;
    private final Executor closeExecutor;
    private final Map<String, TrackedSession> sessions = new ConcurrentHashMap<>();

    private volatile Disposable sweeper;

    public GraphQlWebSocketLivenessMonitor(
        Duration timeout,
        Duration checkInterval,
        Clock clock,
        Executor closeExecutor
    ) {
        Assert.isTrue(timeout.isPositive(), "timeout must be positive");
        Assert.isTrue(checkInterval.isPositive(), "checkInterval must be positive");
        this.timeout = timeout;
        this.checkInterval = checkInterval;
        this.clock = clock;
        this.closeExecutor = closeExecutor;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void messageReceived(WebSocketSession session) {
        Instant now = clock.instant();
        sessions.computeIfAbsent(session.getId(), id -> new TrackedSession(session, now)).touch(now);
    }

    /**
     * Restarts the silence period once a message has been processed, so that slow server-side processing,
     * e.g. a blocking connection initialisation, does not count against the client.
     */
    public void messageHandled(WebSocketSession session) {
        TrackedSession trackedSession = sessions.get(session.getId());
        if (trackedSession != null) {
            trackedSession.touch(clock.instant());
        }
    }

    public void sessionClosed(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public void checkSessions() {
        Instant now = clock.instant();
        sessions.forEach((sessionId, trackedSession) -> {
            if (!trackedSession.session().isOpen()) {
                sessions.remove(sessionId, trackedSession);
            } else if (trackedSession.isSilentFor(timeout, now) && trackedSession.markClosing()) {
                closeExecutor.execute(() -> close(trackedSession, now));
            }
        });
    }

    private void close(TrackedSession trackedSession, Instant now) {
        WebSocketSession session = trackedSession.session();
        LOGGER.debug(
            "Closing GraphQL WebSocket session {}: no message received for {}",
            session.getId(),
            trackedSession.silenceAt(now)
        );
        try {
            session.close(PING_TIMEOUT_STATUS);
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Error while closing silent GraphQL WebSocket session {}", session.getId(), e);
        }
    }

    @Override
    public void start() {
        sweeper = Flux.interval(checkInterval, checkInterval).subscribe(tick -> checkSessionsSafely());
    }

    @Override
    public void stop() {
        Disposable currentSweeper = sweeper;
        if (currentSweeper != null) {
            currentSweeper.dispose();
        }
    }

    @Override
    public boolean isRunning() {
        Disposable currentSweeper = sweeper;
        return currentSweeper != null && !currentSweeper.isDisposed();
    }

    private void checkSessionsSafely() {
        try {
            checkSessions();
        } catch (RuntimeException e) {
            LOGGER.warn("GraphQL WebSocket liveness check failed", e);
        }
    }

    private static final class TrackedSession {

        private final WebSocketSession session;
        private final AtomicBoolean closing = new AtomicBoolean();
        private volatile Instant lastMessageAt;

        private TrackedSession(WebSocketSession session, Instant lastMessageAt) {
            this.session = session;
            this.lastMessageAt = lastMessageAt;
        }

        private WebSocketSession session() {
            return session;
        }

        private void touch(Instant now) {
            lastMessageAt = now;
        }

        private Duration silenceAt(Instant now) {
            return Duration.between(lastMessageAt, now);
        }

        private boolean isSilentFor(Duration timeout, Instant now) {
            return silenceAt(now).compareTo(timeout) >= 0;
        }

        private boolean markClosing() {
            return closing.compareAndSet(false, true);
        }
    }
}
