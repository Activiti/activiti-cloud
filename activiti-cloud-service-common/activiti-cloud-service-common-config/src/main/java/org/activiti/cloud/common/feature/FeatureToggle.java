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

/**
 * Runtime feature-toggle SPI.
 *
 * <p>Implementations are queried on every invocation, so toggling a feature
 * does not require a JVM restart or a Spring bean refresh. This makes the SPI
 * suitable both for property-source backed evaluation (default) and for
 * external feature-flag systems (e.g. Hyland FeatureFlags) provided by
 * downstream modules via a {@code @Primary} bean override.
 *
 * <h2>Flag key naming convention</h2>
 * Implementations and callers MUST use the following dotted key format:
 * <pre>
 *     activiti.features.&lt;area&gt;.&lt;name&gt;
 * </pre>
 * The default {@link EnvironmentFeatureToggle} resolves the property
 * {@code activiti.features.<name>.enabled} from the Spring {@code Environment}
 * on every call. Adapter implementations (e.g. mapping to {@code FFProcess.<NAME>})
 * are expected to translate from this canonical key to their backend key in a
 * single place.
 *
 * <h2>Runtime refresh</h2>
 * The default implementation reads the property source on every call. Combined
 * with Spring Cloud Config and {@code /actuator/refresh}, this allows runtime
 * toggling without {@code @RefreshScope} or bean recreation. Adapter
 * implementations rely on their backend's native runtime evaluation.
 */
public interface FeatureToggle {
    /**
     * @param name canonical feature-toggle name (without the
     *             {@code activiti.features.} prefix or {@code .enabled} suffix)
     * @return {@code true} if the feature is currently enabled
     */
    boolean isEnabled(String name);
}
