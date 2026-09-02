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

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One user's live websocket subscriptions on this query-rest instance.
 *
 * <p>A "session" here is one websocket connection (one browser tab), not one badge
 * subscription - a user with two tabs open has two sessions. {@code groups} is a snapshot
 * taken once, when the user's first session on this instance is added; it is not refreshed
 * for the life of the registration, matching the accepted staleness tradeoff described in
 * the pushed-counts ADR (group membership can drift stale until the connection is reopened).
 */
public class SubscriberRegistration {

    private final String userId;
    private final Set<String> groups;
    private final ConcurrentHashMap<String, Instant> sessions = new ConcurrentHashMap<>();

    public SubscriberRegistration(String userId, Set<String> groups) {
        this.userId = userId;
        this.groups = Set.copyOf(groups);
    }

    public String getUserId() {
        return userId;
    }

    public Set<String> getGroups() {
        return groups;
    }

    /**
     * @return {@code true} if this session was not already present, i.e. adding it was a
     *         0 -&gt; 1 transition for an empty registration.
     */
    public boolean addSession(String sessionId, Instant now) {
        boolean wasEmpty = sessions.isEmpty();
        sessions.put(sessionId, now);
        return wasEmpty;
    }

    /**
     * @return {@code true} if removing this session left the registration with no sessions
     *         at all, i.e. a 1 -&gt; 0 transition.
     */
    public boolean removeSession(String sessionId) {
        sessions.remove(sessionId);
        return sessions.isEmpty();
    }

    /**
     * Refreshes the liveness timestamp of an already-registered session. A no-op if the
     * session id is unknown (e.g. it already expired) - callers must not resurrect a removed
     * session through a stray keep-alive frame.
     */
    public void touch(String sessionId, Instant now) {
        sessions.computeIfPresent(sessionId, (id, previous) -> now);
    }

    public Set<String> expiredSessionIds(Instant now, Duration expiry) {
        Set<String> expired = new HashSet<>();
        sessions.forEach((sessionId, lastSeenAt) -> {
            if (Duration.between(lastSeenAt, now).compareTo(expiry) > 0) {
                expired.add(sessionId);
            }
        });
        return expired;
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public int sessionCount() {
        return sessions.size();
    }
}
