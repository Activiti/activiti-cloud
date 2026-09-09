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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriberInstanceRemovalSchedulerTest {

    @Mock
    private SubscriberInstanceRemover remover;

    private boolean featureEnabled;
    private SubscriberInstanceRemovalScheduler scheduler;

    @BeforeEach
    void setUp() {
        FeatureToggle featureToggle = name -> featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        scheduler = new SubscriberInstanceRemovalScheduler(remover, featureToggle);
    }

    @Test
    void runsRemoval_whenFeatureEnabled() {
        featureEnabled = true;

        scheduler.removeExpiredInstances();

        verify(remover).removeExpiredInstances();
    }

    @Test
    void skipsRemoval_whenFeatureDisabled() {
        featureEnabled = false;

        scheduler.removeExpiredInstances();

        verifyNoInteractions(remover);
    }
}
