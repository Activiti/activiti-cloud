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
 * The local, per-instance {@code userId -> SubscriberRegistration} registry: which users have
 * at least one live websocket session on this query-rest instance.
 *
 * <p>Only the transition from empty to non-empty (a user's first session on this instance) and
 * back to empty (their last session closing) is externally significant - a
 * {@link SubscriberWentLiveEvent} / {@link SubscriberWentQuietEvent} is published exactly then,
 * never on every session add/remove. Both a clean disconnect and the expiry sweep funnel
 * through the same {@link #unregister(String, String, Instant)} method, so there is only ever
 * one code path for "this session is gone".
 *
 * <p>Concurrency: two sessions for the <em>same</em> user can register concurrently (two
 * browser tabs opening within the same instant). {@link ConcurrentHashMap#compute} /
 * {@link ConcurrentHashMap#computeIfPresent} serialize remapping per key, so the
 * "was this the first/last session" decision and the mutation that answers it happen as one
 * atomic step - two concurrent registrations for the same user can never both observe
 * "was empty".
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
        // Best-effort: a size check ahead of the atomic compute below can race with another
        // new registration, so this is a soft cap that can be exceeded by a handful of entries
        // under concurrent load, not a hard invariant - it only exists to keep an unbounded
        // registry from becoming a memory-exhaustion vector.
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

    /**
     * Removes every session past {@code expiry}, taking the exact same path as a clean
     * disconnect ({@link #unregister(String, String, Instant)}) - there is no separate code
     * path for the expiry case.
     */
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
