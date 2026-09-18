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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Phase 2's last step: resolves the audience, runs whatever {@link PushedCounter}s are registered,
 * and publishes one {@link CountChangedMessage} per user per badge onto {@code countProducer}. With
 * zero counters registered - this pipeline's own starting state - the loop below simply does
 * nothing; no counter, no special-casing.
 */
public class RecomputePipeline {

    private final RecomputeAudienceResolver audienceResolver;
    private final Set<PushedCounter> counters;
    private final MessageChannel countProducer;
    private final Clock clock;

    public RecomputePipeline(
        RecomputeAudienceResolver audienceResolver,
        Set<PushedCounter> counters,
        MessageChannel countProducer,
        Clock clock
    ) {
        this.audienceResolver = audienceResolver;
        this.counters = counters;
        this.countProducer = countProducer;
        this.clock = clock;
    }

    public void process(ConsumerRecomputeWindow window) {
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
        for (String userId : affectedUserIds) {
            long count = counts.getOrDefault(userId, 0L);
            String scopeKey = ScopeKeys.of(counter.type(), userId);
            countProducer.send(MessageBuilder.withPayload(new CountChangedMessage(scopeKey, count, asOf)).build());
        }
    }
}
