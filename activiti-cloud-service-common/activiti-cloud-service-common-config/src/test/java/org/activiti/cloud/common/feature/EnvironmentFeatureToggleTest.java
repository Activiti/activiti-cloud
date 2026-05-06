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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class EnvironmentFeatureToggleTest {

    private Map<String, Object> properties;
    private StandardEnvironment environment;
    private EnvironmentFeatureToggle toggle;

    @BeforeEach
    void setUp() {
        properties = new HashMap<>();
        environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        toggle = new EnvironmentFeatureToggle(environment);
    }

    @Test
    void should_return_false_when_property_is_missing() {
        assertThat(toggle.isEnabled("my.feature")).isFalse();
    }

    @Test
    void should_return_true_when_property_is_true() {
        properties.put("activiti.features.my.feature.enabled", "true");

        assertThat(toggle.isEnabled("my.feature")).isTrue();
    }

    @Test
    void should_return_false_when_property_is_false() {
        properties.put("activiti.features.my.feature.enabled", "false");

        assertThat(toggle.isEnabled("my.feature")).isFalse();
    }

    @Test
    void should_re_evaluate_property_on_every_call() {
        assertThat(toggle.isEnabled("my.feature")).isFalse();

        properties.put("activiti.features.my.feature.enabled", "true");
        assertThat(toggle.isEnabled("my.feature")).isTrue();

        properties.put("activiti.features.my.feature.enabled", "false");
        assertThat(toggle.isEnabled("my.feature")).isFalse();
    }

    @Test
    void should_reject_null_feature_name() {
        assertThatNullPointerException().isThrownBy(() -> toggle.isEnabled(null));
    }

    @Test
    void should_reject_null_environment() {
        assertThatNullPointerException().isThrownBy(() -> new EnvironmentFeatureToggle(null));
    }
}
