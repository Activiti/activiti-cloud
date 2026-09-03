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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-connection count of live pushed-counts subscriptions, distinct from
 * {@link SubscriberRegistry}'s per-user session map. {@link PushedCountDataFetcher} calls
 * {@link SubscriberRegistry#register}/{@link SubscriberRegistry#unregister} only on this count's
 * 0 -&gt; 1 / 1 -&gt; 0 transitions.
 */
public class PushedCountsSubscriptionTracker {

    private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    /** @return {@code true} if this was the first live pushed-counts subscription on this session. */
    public boolean incrementAndCheckIfWasZero(String sessionId) {
        AtomicBoolean wasZero = new AtomicBoolean(false);
        counts.compute(sessionId, (id, existing) -> {
            AtomicInteger counter = existing != null ? existing : new AtomicInteger(0);
            wasZero.set(counter.getAndIncrement() == 0);
            return counter;
        });
        return wasZero.get();
    }

    /** @return {@code true} if this was the last live pushed-counts subscription on this session. */
    public boolean decrementAndCheckIfNowZero(String sessionId) {
        AtomicBoolean nowZero = new AtomicBoolean(false);
        counts.computeIfPresent(sessionId, (id, counter) -> {
            boolean isZero = counter.decrementAndGet() <= 0;
            nowZero.set(isZero);
            return isZero ? null : counter;
        });
        return nowZero.get();
    }
}
