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
package org.activiti.services.connectors.mtc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class MtcBinderEnvironmentPostProcessorTest {

    private final MtcBinderEnvironmentPostProcessor postProcessor = new MtcBinderEnvironmentPostProcessor();

    @Test
    void shouldAddMtcBinderPropertiesWhenEnabled() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("mtc.connector.enabled", "true");

        postProcessor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.cloud.stream.default-binder")).isEqualTo("default");
        assertThat(env.getProperty("spring.cloud.stream.binders.default.type")).isEqualTo("rabbit");
        assertThat(env.getProperty("spring.cloud.stream.binders.default.inherit-environment")).isEqualTo("true");
        assertThat(env.getProperty("spring.cloud.stream.binders.default.default-candidate")).isEqualTo("true");
        assertThat(env.getProperty("spring.cloud.stream.binders.mtc.type")).isEqualTo("rabbit");
        assertThat(env.getProperty("spring.cloud.stream.binders.mtc.inherit-environment")).isEqualTo("true");
        assertThat(env.getProperty("spring.cloud.stream.binders.mtc.default-candidate")).isEqualTo("false");
        assertThat(
            env.getProperty(
                "spring.cloud.stream.binders.mtc.environment.spring.cloud.stream.rabbit.default.producer.prefix"
            )
        ).isEmpty();
        assertThat(
            env.getProperty(
                "spring.cloud.stream.binders.mtc.environment.spring.cloud.stream.rabbit.default.consumer.prefix"
            )
        ).isEmpty();
        assertThat(env.getProperty("spring.cloud.stream.bindings.mtcResultsConsumer.binder")).isEqualTo("mtc");
    }

    @Test
    void shouldNotAddPropertiesWhenDisabled() {
        MockEnvironment env = new MockEnvironment();

        postProcessor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.cloud.stream.binders.mtc.type")).isNull();
    }
}
