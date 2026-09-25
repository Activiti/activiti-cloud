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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

class RecomputePipelineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final RecomputeAudienceResolver audienceResolver = mock(RecomputeAudienceResolver.class);
    private final MessageChannel countProducer = mock(MessageChannel.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        when(countProducer.send(any())).thenReturn(true);
    }

    @Test
    void withNoCountersRegistered_doesNothing() {
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(), countProducer, clock);

        pipeline.process(nonEmptyWindow());

        verifyNoInteractions(audienceResolver);
        verify(countProducer, never()).send(any());
    }

    @Test
    void emptyWindow_skipsResolutionEntirely() {
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(), countProducer, clock);

        pipeline.process(emptyWindow());

        verify(audienceResolver, never()).resolve(any());
        verify(countProducer, never()).send(any());
    }

    @Test
    void publishesOneMessagePerAffectedUser_withAbsoluteCountAndFlushTimeAsOf() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of(PushedCountType.ASSIGNED, Set.of("alice", "bob")));
        PushedCounter assignedCounter = counterFor(PushedCountType.ASSIGNED, Map.of("alice", 3L));
        RecomputePipeline pipeline = new RecomputePipeline(
            audienceResolver,
            Set.of(assignedCounter),
            countProducer,
            clock
        );

        pipeline.process(nonEmptyWindow());

        List<CountChangedMessage> sent = capturePayloads();
        assertThat(sent).containsExactlyInAnyOrder(
            new CountChangedMessage("assigned:alice", 3, NOW),
            new CountChangedMessage("assigned:bob", 0, NOW)
        );
    }

    @Test
    void rejectedSend_isTreatedAsAProcessingFailure() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of(PushedCountType.ASSIGNED, Set.of("alice")));
        when(countProducer.send(any())).thenReturn(false);
        PushedCounter assignedCounter = counterFor(PushedCountType.ASSIGNED, Map.of("alice", 3L));
        RecomputePipeline pipeline = new RecomputePipeline(
            audienceResolver,
            Set.of(assignedCounter),
            countProducer,
            clock
        );

        var window = nonEmptyWindow();
        assertThatThrownBy(() -> pipeline.process(window)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void counterWithNoAffectedUsers_isNeverInvoked() {
        when(audienceResolver.resolve(any())).thenReturn(Map.of());
        PushedCounter counter = mock(PushedCounter.class);
        when(counter.type()).thenReturn(PushedCountType.QUEUED);
        RecomputePipeline pipeline = new RecomputePipeline(audienceResolver, Set.of(counter), countProducer, clock);

        pipeline.process(nonEmptyWindow());

        verify(counter, never()).compute(any());
        verify(countProducer, never()).send(any());
    }

    private static PushedCounter counterFor(PushedCountType type, Map<String, Long> counts) {
        PushedCounter counter = mock(PushedCounter.class);
        when(counter.type()).thenReturn(type);
        when(counter.compute(any())).thenReturn(counts);
        return counter;
    }

    private List<CountChangedMessage> capturePayloads() {
        var captor = forClass(Message.class);
        verify(countProducer, atLeastOnce()).send(captor.capture());
        return captor
            .getAllValues()
            .stream()
            .map(m -> (CountChangedMessage) m.getPayload())
            .toList();
    }

    private static ConsumerRecomputeWindow nonEmptyWindow() {
        return new ConsumerRecomputeWindow(Set.of("task-1"), Set.of(), Set.of(), Set.of(), Set.of());
    }

    private static ConsumerRecomputeWindow emptyWindow() {
        return new ConsumerRecomputeWindow(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
