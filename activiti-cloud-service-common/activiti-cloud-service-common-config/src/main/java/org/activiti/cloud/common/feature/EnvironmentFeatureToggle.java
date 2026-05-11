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
import org.springframework.core.env.Environment;

/**
 * Default {@link FeatureToggle} implementation that resolves
 * {@code activiti.features.<name>.enabled} from the Spring {@link Environment}
 * on every call.
 *
 * <p>Re-reading the property source per invocation allows runtime toggling via
 * Spring Cloud Config + {@code /actuator/refresh} (or any other property
 * source reload mechanism) without requiring {@code @RefreshScope} or bean
 * re-creation.
 *
 * <p>The default value when the property is absent is {@code false}.
 */
public class EnvironmentFeatureToggle implements FeatureToggle {

    static final String PROPERTY_PREFIX = "activiti.features.";
    static final String PROPERTY_SUFFIX = ".enabled";

    private final Environment environment;

    public EnvironmentFeatureToggle(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    @Override
    public boolean isEnabled(String name) {
        Objects.requireNonNull(name, "feature name must not be null");
        return environment.getProperty(PROPERTY_PREFIX + name + PROPERTY_SUFFIX, Boolean.class, Boolean.FALSE);
    }
}
