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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumerRecomputeSchedulerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration MAX_WINDOW = Duration.ofSeconds(2);
    private static final int MAX_BATCH_SIZE = 5;

    @Mock
    private RecomputePipeline pipeline;

    private boolean featureEnabled = true;

    @Test
    void skipsEverything_whenFeatureDisabled() {
        featureEnabled = false;
        ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
        buffer.captureTask("task-1", T0);
        ConsumerRecomputeScheduler scheduler = schedulerAt(buffer, T0.plusSeconds(10));

        scheduler.flushIfDue();

        verifyNoInteractions(pipeline);
    }

    @Test
    void doesNothing_whenBufferIsEmpty() {
        ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
        ConsumerRecomputeScheduler scheduler = schedulerAt(buffer, T0.plusSeconds(10));

        scheduler.flushIfDue();

        verifyNoInteractions(pipeline);
    }

    @Test
    void doesNotFlush_beforeTheTimeBound_andBelowTheSizeBound() {
        ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
        buffer.captureTask("task-1", T0);
        ConsumerRecomputeScheduler scheduler = schedulerAt(buffer, T0.plus(MAX_WINDOW.minusMillis(1)));

        scheduler.flushIfDue();

        verify(pipeline, never()).process(any());
    }

    @Test
    void flushes_onceTheTimeBoundIsReached() {
        ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
        buffer.captureTask("task-1", T0);
        ConsumerRecomputeScheduler scheduler = schedulerAt(buffer, T0.plus(MAX_WINDOW));

        scheduler.flushIfDue();

        verify(pipeline).process(any());
        org.assertj.core.api.Assertions.assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void flushesEarly_onceTheSizeBoundIsReached_evenWellWithinTheTimeBound() {
        ConsumerRecomputeBuffer buffer = new ConsumerRecomputeBuffer();
        for (int i = 0; i < MAX_BATCH_SIZE; i++) {
            buffer.captureTask("task-" + i, T0);
        }
        ConsumerRecomputeScheduler scheduler = schedulerAt(buffer, T0.plusMillis(1));

        scheduler.flushIfDue();

        verify(pipeline).process(any());
    }

    private ConsumerRecomputeScheduler schedulerAt(ConsumerRecomputeBuffer buffer, Instant now) {
        FeatureToggle featureToggle = name -> featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        return new ConsumerRecomputeScheduler(buffer, pipeline, featureToggle, clock, MAX_WINDOW, MAX_BATCH_SIZE);
    }
}
