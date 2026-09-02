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
import java.util.UUID;
import java.util.function.Consumer;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.services.query.app.ConsumerSubscriberRegistry;
import org.activiti.cloud.services.query.app.QueryConsumerChannels;
import org.activiti.cloud.services.query.app.SubscriberInstanceRemovalScheduler;
import org.activiti.cloud.services.query.app.SubscriberInstanceRemover;
import org.activiti.cloud.services.query.app.SubscriberRegistryConsumer;
import org.activiti.cloud.services.query.app.SubscriberRegistryMessageHandler;
import org.activiti.cloud.services.query.app.SubscriberRegistryResyncRequester;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the consumer-side subscriber registry to the broker. The registry channel is a fan-out
 * (no consumer group) so every consumer instance builds the full picture of subscribers; the actual
 * broker destinations are configured through {@code spring.cloud.stream.bindings.*} so they can be
 * agreed and changed without code changes.
 */
@AutoConfiguration(after = QueryConsumerAutoConfiguration.class)
@EnableScheduling
public class PushedCountsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ConsumerSubscriberRegistry consumerSubscriberRegistry() {
        return new ConsumerSubscriberRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    SubscriberRegistryMessageHandler subscriberRegistryMessageHandler(ConsumerSubscriberRegistry registry) {
        return new SubscriberRegistryMessageHandler(registry);
    }

    @Bean
    @FunctionBinding(input = QueryConsumerChannels.SUBSCRIBER_REGISTRY_CONSUMER)
    public Consumer<Message<SubscriberRegistryMessage>> subscriberRegistryConsumerFunction(
        SubscriberRegistryMessageHandler handler,
        FeatureToggle featureToggle
    ) {
        return new SubscriberRegistryConsumer(handler, featureToggle);
    }

    @Bean
    @ConditionalOnMissingBean
    SubscriberInstanceRemover subscriberInstanceRemover(
        ConsumerSubscriberRegistry registry,
        @Value("${activiti.cloud.query.pushed-counts.instance-timeout:PT3M}") Duration instanceTimeout
    ) {
        return new SubscriberInstanceRemover(registry, instanceTimeout, Clock.systemUTC());
    }

    @Bean
    SubscriberInstanceRemovalScheduler subscriberInstanceRemovalScheduler(
        SubscriberInstanceRemover remover,
        FeatureToggle featureToggle
    ) {
        return new SubscriberInstanceRemovalScheduler(remover, featureToggle);
    }

    @Bean
    SubscriberRegistryResyncRequester subscriberRegistryResyncRequester(
        @Qualifier(QueryConsumerChannels.SUBSCRIBER_REGISTRY_PRODUCER) MessageChannel registryProducer,
        FeatureToggle featureToggle
    ) {
        return new SubscriberRegistryResyncRequester(
            registryProducer,
            featureToggle,
            UUID.randomUUID().toString(),
            Clock.systemUTC()
        );
    }
}
