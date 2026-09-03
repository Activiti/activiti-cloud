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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriberInstanceRemovalSchedulerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    private ConsumerSubscriberRegistry registry;
    private boolean featureEnabled;
    private SubscriberInstanceRemovalScheduler scheduler;

    @BeforeEach
    void setUp() {
        registry = new ConsumerSubscriberRegistry();
        registry.register("alice", List.of("eng"), "rest-1", T0);
        // clock is 4 minutes past T0, so rest-1 is beyond the 3-minute timeout
        SubscriberInstanceRemover remover = new SubscriberInstanceRemover(
            registry,
            TIMEOUT,
            Clock.fixed(T0.plus(Duration.ofMinutes(4)), ZoneOffset.UTC)
        );
        FeatureToggle featureToggle = name -> featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        scheduler = new SubscriberInstanceRemovalScheduler(remover, featureToggle);
    }

    @Test
    void removesExpiredInstances_whenFeatureEnabled() {
        featureEnabled = true;

        scheduler.removeExpiredInstances();

        assertThat(registry.isWatching("alice")).isFalse();
    }

    @Test
    void doesNothing_whenFeatureDisabled() {
        featureEnabled = false;

        scheduler.removeExpiredInstances();

        assertThat(registry.isWatching("alice")).isTrue();
    }
}
