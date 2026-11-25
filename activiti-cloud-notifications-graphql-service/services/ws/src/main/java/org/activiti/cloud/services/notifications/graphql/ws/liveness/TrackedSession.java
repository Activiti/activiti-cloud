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
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.socket.WebSocketSession;

final class TrackedSession {

    private final WebSocketSession session;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicInteger messagesInProgress = new AtomicInteger();
    private volatile long lastMessageAt;

    TrackedSession(WebSocketSession session, long lastMessageAt) {
        this.session = session;
        this.lastMessageAt = lastMessageAt;
    }

    WebSocketSession session() {
        return session;
    }

    void messageStarted(long now) {
        lastMessageAt = now;
        messagesInProgress.incrementAndGet();
    }

    void messageFinished(long now) {
        lastMessageAt = now;
        messagesInProgress.updateAndGet(count -> Math.max(0, count - 1));
    }

    Duration silenceAt(long now) {
        return Duration.ofNanos(now - lastMessageAt);
    }

    boolean isSilentFor(Duration timeout, long now) {
        return messagesInProgress.get() == 0 && silenceAt(now).compareTo(timeout) >= 0;
    }

    boolean markClosing() {
        return closing.compareAndSet(false, true);
    }

    void unmarkClosing() {
        closing.set(false);
    }
}
