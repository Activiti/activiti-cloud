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

import java.util.Objects;

/**
 * Static accessor for the application-wide {@link FeatureToggle} bean.
 *
 * <p>Allows non-Spring-managed components (e.g. JPA {@code Specification} implementations
 * created on the fly) to query feature flags without having to be reworked to accept a
 * {@link FeatureToggle} via constructor injection.
 *
 * <p>The holder is initialized at application startup by Spring (see
 * {@link FeatureToggleAutoConfiguration}). Until initialization runs, every flag resolves to
 * {@code false} so behavior defaults to the pre-feature-toggle (legacy) code path.
 */
public final class FeatureToggleHolder {

    /**
     * Default no-op {@link FeatureToggle} that disables every flag. Used until the Spring-managed
     * {@link FeatureToggle} bean is wired in by {@link #initialize(FeatureToggle)}.
     */
    private static volatile FeatureToggle instance = name -> false;

    private FeatureToggleHolder() {}

    /**
     * Sets the application-wide {@link FeatureToggle}. Invoked by Spring during startup.
     * Visible for tests so they can install a custom toggle and reset it via
     * {@link #reset()} in {@code @AfterEach}.
     */
    public static void initialize(FeatureToggle featureToggle) {
        instance = Objects.requireNonNull(featureToggle, "featureToggle must not be null");
    }

    /** Restores the default no-op toggle. Intended for test teardown. */
    public static void reset() {
        instance = name -> false;
    }

    /**
     * @param name canonical feature-toggle name (without the {@code activiti.features.} prefix
     *             or {@code .enabled} suffix)
     * @return {@code true} if the feature is currently enabled, {@code false} otherwise (also
     *         when the holder has not yet been initialized).
     */
    public static boolean isEnabled(String name) {
        return instance.isEnabled(name);
    }
}
