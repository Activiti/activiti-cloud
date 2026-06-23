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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

class FeatureToggleAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(FeatureToggleAutoConfiguration.class)
    );

    @AfterEach
    void tearDown() {
        FeatureToggleHolder.reset();
    }

    @Test
    void should_register_default_environment_feature_toggle() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FeatureToggle.class);
            assertThat(context.getBean(FeatureToggle.class)).isInstanceOf(EnvironmentFeatureToggle.class);
        });
    }

    @Test
    void should_evaluate_property_through_auto_configured_bean() {
        contextRunner
            .withPropertyValues("activiti.features.sample.enabled=true")
            .run(context -> {
                FeatureToggle toggle = context.getBean(FeatureToggle.class);
                assertThat(toggle.isEnabled("sample")).isTrue();
                assertThat(toggle.isEnabled("missing")).isFalse();
            });
    }

    @Test
    void should_back_off_when_primary_override_is_provided() {
        contextRunner
            .withUserConfiguration(PrimaryOverrideConfiguration.class)
            .run(context -> {
                assertThat(context).getBeans(FeatureToggle.class).hasSize(1);
                FeatureToggle toggle = context.getBean(FeatureToggle.class);
                assertThat(toggle).isNotInstanceOf(EnvironmentFeatureToggle.class);
                assertThat(toggle.isEnabled("anything")).isTrue();
            });
    }

    @Test
    void should_initialize_static_holder_with_application_feature_toggle() {
        contextRunner
            .withPropertyValues("activiti.features.holder-sample.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(FeatureToggleHolderInitializer.class);
                assertThat(FeatureToggleHolder.isEnabled("holder-sample")).isTrue();
                assertThat(FeatureToggleHolder.isEnabled("missing")).isFalse();
            });
    }

    @Test
    void should_initialize_static_holder_with_primary_override() {
        contextRunner
            .withUserConfiguration(PrimaryOverrideConfiguration.class)
            .run(context -> assertThat(FeatureToggleHolder.isEnabled("anything")).isTrue());
    }

    @Configuration(proxyBeanMethods = false)
    static class PrimaryOverrideConfiguration {

        @Bean
        @Primary
        FeatureToggle alwaysOnFeatureToggle() {
            return name -> true;
        }
    }
}
