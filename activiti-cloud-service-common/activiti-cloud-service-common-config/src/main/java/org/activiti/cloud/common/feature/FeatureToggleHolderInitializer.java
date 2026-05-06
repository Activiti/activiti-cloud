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
package org.activiti.cloud.common.feature;

import org.springframework.beans.factory.InitializingBean;

/**
 * Bean that initializes the static {@link FeatureToggleHolder} with the
 * Spring-managed {@link FeatureToggle} as soon as it is constructed.
 */
public class FeatureToggleHolderInitializer implements InitializingBean {

    private final FeatureToggle featureToggle;

    public FeatureToggleHolderInitializer(FeatureToggle featureToggle) {
        this.featureToggle = featureToggle;
    }

    @Override
    public void afterPropertiesSet() {
        FeatureToggleHolder.initialize(featureToggle);
    }
}
