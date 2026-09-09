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

import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically drives {@link SubscriberInstanceRemover} so instances that stopped sending
 * heartbeats are reclaimed. Runs only while the pushed-counts toggle is on, so it can be switched
 * off at runtime without a redeploy.
 */
public class SubscriberInstanceRemovalScheduler {

    private final SubscriberInstanceRemover remover;
    private final FeatureToggle featureToggle;

    public SubscriberInstanceRemovalScheduler(SubscriberInstanceRemover remover, FeatureToggle featureToggle) {
        this.remover = remover;
        this.featureToggle = featureToggle;
    }

    @Scheduled(fixedDelayString = "${activiti.cloud.query.pushed-counts.removal-interval:PT1M}")
    public void removeExpiredInstances() {
        if (featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            remover.removeExpiredInstances();
        }
    }
}
