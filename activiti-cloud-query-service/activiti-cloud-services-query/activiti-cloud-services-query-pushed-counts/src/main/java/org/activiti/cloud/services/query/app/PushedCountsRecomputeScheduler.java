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
import java.time.Duration;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Polls well under the window bound; drains and runs the pipeline once the buffer crosses its time
 * or size bound. Frequent polling lets a burst flush early instead of waiting out the window.
 */
public class PushedCountsRecomputeScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushedCountsRecomputeScheduler.class);

    private final PushedCountsRecomputeBuffer buffer;
    private final RecomputePipeline pipeline;
    private final FeatureToggle featureToggle;
    private final Clock clock;
    private final Duration maxWindow;
    private final int maxBatchSize;

    public PushedCountsRecomputeScheduler(
        PushedCountsRecomputeBuffer buffer,
        RecomputePipeline pipeline,
        FeatureToggle featureToggle,
        Clock clock,
        Duration maxWindow,
        int maxBatchSize
    ) {
        this.buffer = buffer;
        this.pipeline = pipeline;
        this.featureToggle = featureToggle;
        this.clock = clock;
        this.maxWindow = maxWindow;
        this.maxBatchSize = maxBatchSize;
    }

    @Scheduled(fixedDelayString = "${activiti.cloud.query.pushed-counts.flush-interval:PT0.25S}")
    public void flushIfDue() {
        if (!featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            return;
        }
        if (buffer.isEmpty()) {
            return;
        }
        if (buffer.age(clock).compareTo(maxWindow) >= 0 || buffer.size() >= maxBatchSize) {
            PushedCountsRecomputeWindow window = buffer.drainAndReset();
            LOGGER.debug(
                "Flushing a recompute window: {} tasks, {} processes touched",
                window.taskIds().size(),
                window.processInstanceIds().size()
            );
            try {
                pipeline.process(window);
            } catch (RuntimeException e) {
                buffer.mergeBack(window, clock.instant());
                LOGGER.warn("Failed to process a recompute window; retrying on the next flush", e);
            }
        }
    }
}
