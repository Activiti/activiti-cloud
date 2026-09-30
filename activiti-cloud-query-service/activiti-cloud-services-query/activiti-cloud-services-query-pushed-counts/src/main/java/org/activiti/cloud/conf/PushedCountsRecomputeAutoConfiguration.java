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

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.AssignedTaskCounter;
import org.activiti.cloud.services.query.app.PushedCountsQueryEventsConsumer;
import org.activiti.cloud.services.query.app.PushedCountsRecomputeBuffer;
import org.activiti.cloud.services.query.app.PushedCountsRecomputeChannels;
import org.activiti.cloud.services.query.app.PushedCountsRecomputeScheduler;
import org.activiti.cloud.services.query.app.RecomputeAudienceResolver;
import org.activiti.cloud.services.query.app.RecomputeEventCapturer;
import org.activiti.cloud.services.query.app.RecomputePipeline;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.SubscriberDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Sinks;

/**
 * Wires this instance's own recompute pipeline: its own {@code queryEvents} subscription, its own
 * capture/coalesce buffer, its own audience resolution against whatever {@link SubscriberDirectory}
 * bean is present (query-rest's {@code SubscriberRegistry}), its own counters, and direct in-process
 * delivery into the {@code Sinks.Many<CountChangedMessage>} bean query-rest's websocket
 * autoconfiguration already defines - hence the ordering below.
 *
 * <p>Gated at startup by {@code activiti.cloud.query.pushed-counts.enabled}: when off, none of these
 * beans are wired.
 */
@AutoConfiguration(afterName = "org.activiti.cloud.conf.QueryRestPushedCountsWebSocketAutoConfiguration")
@ConditionalOnProperty(
    name = "activiti.cloud.query.pushed-counts.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@PropertySource("classpath:pushed-counts-recompute-messaging.properties")
@EnableScheduling
@Import(PushedCountsRecomputeChannelsConfiguration.class)
public class PushedCountsRecomputeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PushedCountsRecomputeBuffer pushedCountsRecomputeBuffer() {
        return new PushedCountsRecomputeBuffer();
    }

    @Bean
    @ConditionalOnMissingBean
    RecomputeEventCapturer recomputeEventCapturer(
        PushedCountsRecomputeBuffer buffer,
        FeatureToggle featureToggle,
        Clock clock
    ) {
        return new RecomputeEventCapturer(buffer, featureToggle, clock);
    }

    @Bean
    @FunctionBinding(input = PushedCountsRecomputeChannels.QUERY_EVENTS_CONSUMER)
    public Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> pushedCountsQueryEventsConsumerFunction(
        RecomputeEventCapturer recomputeEventCapturer
    ) {
        return new PushedCountsQueryEventsConsumer(recomputeEventCapturer);
    }

    @Bean
    @ConditionalOnMissingBean
    RecomputeAudienceResolver recomputeAudienceResolver(
        SubscriberDirectory registry,
        TaskCandidateUserRepository taskCandidateUserRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository,
        TaskRepository taskRepository
    ) {
        return new RecomputeAudienceResolver(
            registry,
            taskCandidateUserRepository,
            taskCandidateGroupRepository,
            taskRepository
        );
    }

    @Bean
    @ConditionalOnMissingBean
    RecomputePipeline recomputePipeline(
        RecomputeAudienceResolver audienceResolver,
        Set<PushedCounter> counters,
        Sinks.Many<CountChangedMessage> pushedCountsSink,
        Clock clock
    ) {
        return new RecomputePipeline(audienceResolver, counters, pushedCountsSink, clock);
    }

    @Bean
    PushedCountsRecomputeScheduler pushedCountsRecomputeScheduler(
        PushedCountsRecomputeBuffer buffer,
        RecomputePipeline pipeline,
        FeatureToggle featureToggle,
        Clock clock,
        @Value("${activiti.cloud.query.pushed-counts.flush-max-window:PT2S}") Duration maxWindow,
        @Value("${activiti.cloud.query.pushed-counts.flush-max-size:2000}") int maxBatchSize
    ) {
        return new PushedCountsRecomputeScheduler(buffer, pipeline, featureToggle, clock, maxWindow, maxBatchSize);
    }

    @Bean
    @ConditionalOnMissingBean
    AssignedTaskCounter assignedTaskCounter(TaskRepository taskRepository) {
        return new AssignedTaskCounter(taskRepository);
    }
}
