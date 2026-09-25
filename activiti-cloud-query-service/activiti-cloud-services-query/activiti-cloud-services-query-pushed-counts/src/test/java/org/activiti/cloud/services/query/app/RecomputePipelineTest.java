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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

class RecomputePipelineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final RecomputeAudienceResolver audienceResolver = mock(RecomputeAudienceResolver.class);
    private final Sinks.Many<CountChangedMessage> pushedCountsSink = Sinks.many().multicast().onBackpressureBuffer();
    private final List<CountChangedMessage> received = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    RecomputePipelineTest() {
        pushedCountsSink.asFlux().subscribe(received::add);
    }

    @Test
    void withNoCountersRegistered_doesNothing() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of(PushedCountType.ASSIGNED, Set.of("alice")));
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(), pushedCountsSink, clock);

        pipeline.process(nonEmptyWindow());

        assertThat(received).isEmpty();
    }

    @Test
    void emptyWindow_skipsResolutionEntirely() {
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(), pushedCountsSink, clock);

        pipeline.process(emptyWindow());

        verify(audienceResolver, never()).resolve(any());
        assertThat(received).isEmpty();
    }

    @Test
    void publishesOneMessagePerAffectedUser_withAbsoluteCountAndFlushTimeAsOf() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of(PushedCountType.ASSIGNED, Set.of("alice", "bob")));
        PushedCounter assignedCounter = counterFor(PushedCountType.ASSIGNED, Map.of("alice", 3L));
        RecomputePipeline pipeline = new RecomputePipeline(
            audienceResolver,
            Set.of(assignedCounter),
            pushedCountsSink,
            clock
        );

        pipeline.process(nonEmptyWindow());

        assertThat(received).containsExactlyInAnyOrder(
            new CountChangedMessage("assigned:alice", 3, NOW),
            new CountChangedMessage("assigned:bob", 0, NOW)
        );
    }

    @Test
    void aFailedEmit_isLoggedAndSkipped_ratherThanThrown() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of(PushedCountType.ASSIGNED, Set.of("alice")));
        PushedCounter assignedCounter = counterFor(PushedCountType.ASSIGNED, Map.of("alice", 3L));
        RecomputePipeline pipeline = new RecomputePipeline(
            audienceResolver,
            Set.of(assignedCounter),
            pushedCountsSink,
            clock
        );
        pushedCountsSink.tryEmitComplete();

        assertThatNoException().isThrownBy(() -> pipeline.process(nonEmptyWindow()));
    }

    @Test
    void counterWithNoAffectedUsers_isNeverInvoked() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of());
        PushedCounter counter = mock(PushedCounter.class);
        when(counter.type()).thenReturn(PushedCountType.QUEUED);
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(counter), pushedCountsSink, clock);

        pipeline.process(nonEmptyWindow());

        verify(counter, never()).compute(any());
        assertThat(received).isEmpty();
    }

    private static PushedCounter counterFor(PushedCountType type, Map<String, Long> counts) {
        PushedCounter counter = mock(PushedCounter.class);
        when(counter.type()).thenReturn(type);
        when(counter.compute(any())).thenReturn(counts);
        return counter;
    }

    private static PushedCountsRecomputeWindow nonEmptyWindow() {
        return new PushedCountsRecomputeWindow(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());
    }

    private static PushedCountsRecomputeWindow emptyWindow() {
        return new PushedCountsRecomputeWindow(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
