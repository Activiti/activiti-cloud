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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.events.config.EventHandlersAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class QueryPayloadCleanupConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(EventHandlersAutoConfiguration.class)
        .withBean(EntityManager.class, () -> mock(EntityManager.class))
        .withBean(ApplicationRepository.class, () -> mock(ApplicationRepository.class));

    @Test
    void should_preserveExistingRetention_whenPolicyIsAbsent() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(QueryPayloadCleanup.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "keep", "transient" })
    void should_selectCleanupByPolicy(String policy) {
        runner.withPropertyValues("activiti.cloud.processing-data.retention-policy=" + policy).run(context -> {
            assertThat(context).hasNotFailed();
            if (policy.equals("transient")) {
                assertThat(context).hasSingleBean(QueryPayloadCleanup.class);
                assertThat(context.getBean(QueryPayloadCleanup.class)).isInstanceOf(TransientQueryPayloadCleanup.class);
            } else {
                assertThat(context).doesNotHaveBean(QueryPayloadCleanup.class);
            }
        });
    }

    @Test
    void should_allowReplacementStrategy() {
        var replacement = mock(QueryPayloadCleanup.class);
        runner
            .withPropertyValues("activiti.cloud.processing-data.retention-policy=transient")
            .withBean("replacementCleanup", QueryPayloadCleanup.class, () -> replacement)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(QueryPayloadCleanup.class);
                assertThat(context.getBean(QueryPayloadCleanup.class)).isSameAs(replacement);
            });
    }
}
