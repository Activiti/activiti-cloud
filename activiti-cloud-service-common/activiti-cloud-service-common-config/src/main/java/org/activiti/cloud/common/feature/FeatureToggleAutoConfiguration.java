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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Registers the default {@link FeatureToggle} bean.
 *
 * <p>Downstream modules (e.g. the hxp-process-services adapter to the Hyland
 * FeatureFlags library) are expected to override this bean by contributing
 * their own {@code @Primary} {@link FeatureToggle} implementation, in which
 * case the default {@link EnvironmentFeatureToggle} acts as a safety net but
 * is not injected.
 */
@AutoConfiguration
public class FeatureToggleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FeatureToggle.class)
    public FeatureToggle environmentFeatureToggle(Environment environment) {
        return new EnvironmentFeatureToggle(environment);
    }

    /**
     * Bridges the Spring-managed {@link FeatureToggle} bean into the static
     * {@link FeatureToggleHolder} so non-Spring-managed components (e.g. JPA
     * specifications instantiated on the fly) can query feature flags without being
     * reworked to accept a {@link FeatureToggle} via constructor injection.
     */
    @Bean
    public FeatureToggleHolderInitializer featureToggleHolderInitializer(FeatureToggle featureToggle) {
        return new FeatureToggleHolderInitializer(featureToggle);
    }
}
