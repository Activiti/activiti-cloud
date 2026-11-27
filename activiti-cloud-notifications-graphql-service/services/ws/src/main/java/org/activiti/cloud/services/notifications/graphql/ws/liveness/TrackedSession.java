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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.socket.WebSocketSession;

final class TrackedSession {

    private final WebSocketSession session;
    private final AtomicBoolean closeScheduled = new AtomicBoolean();
    private long lastMessageAt;
    private int messagesInProgress;
    private boolean closeCommitted;

    TrackedSession(WebSocketSession session, long lastMessageAt) {
        this.session = session;
        this.lastMessageAt = lastMessageAt;
    }

    WebSocketSession session() {
        return session;
    }

    synchronized boolean messageStarted(long now) {
        if (closeCommitted) {
            return false;
        }
        lastMessageAt = now;
        messagesInProgress++;
        return true;
    }

    synchronized void messageFinished(long now) {
        lastMessageAt = now;
        messagesInProgress = Math.max(0, messagesInProgress - 1);
    }

    synchronized Duration silenceAt(long now) {
        return Duration.ofNanos(now - lastMessageAt);
    }

    synchronized boolean isSilentFor(Duration timeout, long now) {
        return messagesInProgress == 0 && silenceAt(now).compareTo(timeout) >= 0;
    }

    synchronized boolean commitClose(Duration timeout, long now) {
        if (!closeCommitted) {
            closeCommitted = isSilentFor(timeout, now);
        }
        return closeCommitted;
    }

    boolean markCloseScheduled() {
        return closeScheduled.compareAndSet(false, true);
    }

    void unmarkCloseScheduled() {
        closeScheduled.set(false);
    }
}
