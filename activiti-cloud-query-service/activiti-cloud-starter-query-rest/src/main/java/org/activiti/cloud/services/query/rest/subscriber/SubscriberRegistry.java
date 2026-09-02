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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The local, per-instance {@code userId -> SubscriberRegistration} registry. Publishes a
 * {@link SubscriberWentLiveEvent} / {@link SubscriberWentQuietEvent} only on the empty/non-empty
 * transition, never on every session add/remove; a clean disconnect and the expiry sweep both go
 * through {@link #unregister(String, String, Instant)}.
 *
 * <p>Concurrency: {@link ConcurrentHashMap#compute}/{@code computeIfPresent} serialize remapping
 * per key, so two sessions for the same user registering concurrently can never both observe
 * "was empty" and double-fire a transition.
 */
public class SubscriberRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistry.class);

    private final ConcurrentHashMap<String, SubscriberRegistration> registrations = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final long maxSize;

    public SubscriberRegistry(ApplicationEventPublisher eventPublisher, long maxSize) {
        this.eventPublisher = eventPublisher;
        this.maxSize = maxSize;
    }

    public void register(String userId, Set<String> groups, String sessionId, Instant now) {
        // Soft cap - this check races with concurrent registrations and can be exceeded
        // slightly; it only guards against unbounded growth, not exactness.
        if (!registrations.containsKey(userId) && registrations.size() >= maxSize) {
            LOGGER.warn(
                "Subscriber registry is at its configured maximum size ({}); not registering a new session for user {}",
                maxSize,
                userId
            );
            return;
        }
        AtomicBoolean wentLive = new AtomicBoolean(false);
        registrations.compute(userId, (id, existing) -> {
            SubscriberRegistration registration =
                existing != null ? existing : new SubscriberRegistration(userId, groups);
            wentLive.set(registration.addSession(sessionId, now));
            return registration;
        });
        if (wentLive.get()) {
            eventPublisher.publishEvent(new SubscriberWentLiveEvent(userId, groups, now));
        }
    }

    public void unregister(String userId, String sessionId, Instant now) {
        AtomicBoolean wentQuiet = new AtomicBoolean(false);
        registrations.computeIfPresent(userId, (id, registration) -> {
            boolean isEmpty = registration.removeSession(sessionId);
            wentQuiet.set(isEmpty);
            return isEmpty ? null : registration;
        });
        if (wentQuiet.get()) {
            eventPublisher.publishEvent(new SubscriberWentQuietEvent(userId, now));
        }
    }

    public void touch(String userId, String sessionId, Instant now) {
        registrations.computeIfPresent(userId, (id, registration) -> {
            registration.touch(sessionId, now);
            return registration;
        });
    }

    /** Removes sessions past {@code expiry} via the same path as a clean disconnect. */
    public void expireSessionsOlderThan(Duration expiry, Instant now) {
        registrations.forEach((userId, registration) -> {
            for (String sessionId : registration.expiredSessionIds(now, expiry)) {
                unregister(userId, sessionId, now);
            }
        });
    }

    public int size() {
        return registrations.size();
    }
}
