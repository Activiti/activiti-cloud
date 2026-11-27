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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public class GraphQlWebSocketLivenessMonitor implements SmartLifecycle {

    public static final CloseStatus PING_TIMEOUT_STATUS = new CloseStatus(4410, "Ping timeout");

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlWebSocketLivenessMonitor.class);

    private final Duration timeout;
    private final Duration checkInterval;
    private final LongSupplier nanoTime;
    private final Executor closeExecutor;
    private final Map<String, TrackedSession> sessions = new ConcurrentHashMap<>();

    private Disposable sweeper;

    public GraphQlWebSocketLivenessMonitor(
        Duration timeout,
        Duration checkInterval,
        LongSupplier nanoTime,
        Executor closeExecutor
    ) {
        this.timeout = timeout;
        this.checkInterval = checkInterval;
        this.nanoTime = nanoTime;
        this.closeExecutor = closeExecutor;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public boolean messageReceived(WebSocketSession session) {
        // A session being closed may already be untracked, so a new entry must not readmit its messages
        if (!session.isOpen()) {
            return false;
        }
        long now = nanoTime.getAsLong();
        return sessions.computeIfAbsent(session.getId(), id -> new TrackedSession(session, now)).messageStarted(now);
    }

    public void messageHandled(WebSocketSession session) {
        TrackedSession trackedSession = sessions.get(session.getId());
        if (trackedSession != null) {
            trackedSession.messageFinished(nanoTime.getAsLong());
        }
    }

    public void sessionClosed(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public void checkSessions() {
        long now = nanoTime.getAsLong();
        sessions.forEach((sessionId, trackedSession) -> {
            if (!trackedSession.session().isOpen()) {
                sessions.remove(sessionId, trackedSession);
            } else if (trackedSession.isSilentFor(timeout, now) && trackedSession.markCloseScheduled()) {
                try {
                    closeExecutor.execute(() -> close(trackedSession));
                } catch (RejectedExecutionException e) {
                    trackedSession.unmarkCloseScheduled();
                    LOGGER.debug("Closing silent GraphQL WebSocket session {} rejected, will retry", sessionId, e);
                }
            }
        });
    }

    private void close(TrackedSession trackedSession) {
        long now = nanoTime.getAsLong();
        // A message may have arrived since the session was found silent; once committed, no new message is admitted
        if (!trackedSession.commitClose(timeout, now)) {
            trackedSession.unmarkCloseScheduled();
            return;
        }
        WebSocketSession session = trackedSession.session();
        LOGGER.info(
            "Closing GraphQL WebSocket session {}: no message received for {}",
            session.getId(),
            trackedSession.silenceAt(now)
        );
        try {
            session.close(PING_TIMEOUT_STATUS);
        } catch (IOException | RuntimeException e) {
            trackedSession.unmarkCloseScheduled();
            LOGGER.debug("Error while closing silent GraphQL WebSocket session {}", session.getId(), e);
        }
    }

    @Override
    public synchronized void start() {
        if (!isRunning()) {
            sweeper = Flux.interval(checkInterval, checkInterval).subscribe(tick -> checkSessionsSafely());
        }
    }

    @Override
    public synchronized void stop() {
        if (sweeper != null) {
            sweeper.dispose();
            sweeper = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return sweeper != null && !sweeper.isDisposed();
    }

    private void checkSessionsSafely() {
        try {
            checkSessions();
        } catch (RuntimeException e) {
            LOGGER.warn("GraphQL WebSocket liveness check failed", e);
        }
    }
}
