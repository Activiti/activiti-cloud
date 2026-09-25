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
package org.activiti.cloud.services.query.app;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/**
 * Resolves the audience, runs whatever {@link PushedCounter}s are registered, and publishes one
 * {@link CountChangedMessage} per user per badge into the local {@code pushedCountsSink}. With no
 * counters registered, this does nothing.
 */
public class RecomputePipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecomputePipeline.class);

    private final RecomputeAudienceResolver audienceResolver;
    private final Set<PushedCounter> counters;
    private final Sinks.Many<CountChangedMessage> pushedCountsSink;
    private final Clock clock;

    public RecomputePipeline(
        RecomputeAudienceResolver audienceResolver,
        Set<PushedCounter> counters,
        Sinks.Many<CountChangedMessage> pushedCountsSink,
        Clock clock
    ) {
        this.audienceResolver = audienceResolver;
        this.counters = counters;
        this.pushedCountsSink = pushedCountsSink;
        this.clock = clock;
    }

    public void process(PushedCountsRecomputeWindow window) {
        if (window.isEmpty()) {
            return;
        }
        Map<PushedCountType, Set<String>> audience = audienceResolver.resolve(window);
        Instant asOf = clock.instant();
        for (PushedCounter counter : counters) {
            publish(counter, audience.getOrDefault(counter.type(), Set.of()), asOf);
        }
    }

    private void publish(PushedCounter counter, Set<String> affectedUserIds, Instant asOf) {
        if (affectedUserIds.isEmpty()) {
            return;
        }
        Map<String, Long> counts = counter.compute(affectedUserIds);
        LOGGER.debug("Publishing {} for {} affected users", counter.type(), affectedUserIds.size());
        for (String userId : affectedUserIds) {
            long count = counts.getOrDefault(userId, 0L);
            String scopeKey = ScopeKeys.of(counter.type(), userId);
            Sinks.EmitResult result = pushedCountsSink.tryEmitNext(new CountChangedMessage(scopeKey, count, asOf));
            if (result.isFailure()) {
                LOGGER.warn("Failed to emit a pushed-count update for {}: {}", scopeKey, result);
            }
        }
    }
}
