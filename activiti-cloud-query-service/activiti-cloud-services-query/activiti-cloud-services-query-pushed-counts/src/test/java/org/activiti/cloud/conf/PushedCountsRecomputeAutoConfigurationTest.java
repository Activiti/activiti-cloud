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
package org.activiti.cloud.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.app.AssignedTaskCounter;
import org.activiti.cloud.services.query.app.PushedCountsRecomputeBuffer;
import org.activiti.cloud.services.query.app.PushedCountsRecomputeScheduler;
import org.activiti.cloud.services.query.app.RecomputeAudienceResolver;
import org.activiti.cloud.services.query.app.RecomputeEventCapturer;
import org.activiti.cloud.services.query.app.RecomputePipeline;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.SubscriberDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Sinks;

class PushedCountsRecomputeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context ->
            context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance())
        )
        .withBean(Clock.class, Clock::systemUTC)
        .withBean(SubscriberDirectory.class, () -> mock(SubscriberDirectory.class))
        .withBean(TaskCandidateUserRepository.class, () -> mock(TaskCandidateUserRepository.class))
        .withBean(TaskCandidateGroupRepository.class, () -> mock(TaskCandidateGroupRepository.class))
        .withBean(TaskRepository.class, () -> mock(TaskRepository.class))
        .withBean(FeatureToggle.class, () -> name -> false)
        .withBean("pushedCountsSink", Sinks.Many.class, () -> Sinks.many().multicast().onBackpressureBuffer())
        .withPropertyValues("activiti.cloud.query.pushed-counts.enabled=true")
        .withConfiguration(AutoConfigurations.of(PushedCountsRecomputeAutoConfiguration.class));

    @Test
    void registersAllRecomputePipelineBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PushedCountsRecomputeBuffer.class);
            assertThat(context).hasSingleBean(RecomputeEventCapturer.class);
            assertThat(context).hasSingleBean(RecomputeAudienceResolver.class);
            assertThat(context).hasSingleBean(RecomputePipeline.class);
            assertThat(context).hasSingleBean(PushedCountsRecomputeScheduler.class);
            assertThat(context).hasSingleBean(AssignedTaskCounter.class);
            assertThat(context).hasBean("pushedCountsQueryEventsConsumerFunction");
        });
    }

    @Test
    void backsOffEntirely_whenStartupPropertyIsDisabled() {
        contextRunner.withPropertyValues("activiti.cloud.query.pushed-counts.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AssignedTaskCounter.class);
            assertThat(context).doesNotHaveBean(RecomputePipeline.class);
            assertThat(context).doesNotHaveBean("pushedCountsQueryEventsConsumerFunction");
        });
    }

    @Test
    void backsOffBuffer_whenOneIsAlreadyDefined() {
        PushedCountsRecomputeBuffer existing = new PushedCountsRecomputeBuffer();

        contextRunner
            .withBean("customBuffer", PushedCountsRecomputeBuffer.class, () -> existing)
            .run(context -> {
                assertThat(context).hasSingleBean(PushedCountsRecomputeBuffer.class);
                assertThat(context.getBean(PushedCountsRecomputeBuffer.class)).isSameAs(existing);
            });
    }
}
