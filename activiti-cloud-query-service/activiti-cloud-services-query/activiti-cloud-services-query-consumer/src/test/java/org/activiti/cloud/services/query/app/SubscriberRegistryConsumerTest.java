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
package org.activiti.cloud.services.query.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

class SubscriberRegistryConsumerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private ConsumerSubscriberRegistry registry;
    private boolean featureEnabled;
    private SubscriberRegistryConsumer consumer;

    @BeforeEach
    void setUp() {
        registry = new ConsumerSubscriberRegistry();
        SubscriberRegistryMessageHandler handler = new SubscriberRegistryMessageHandler(registry);
        FeatureToggle featureToggle = name ->
            featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        consumer = new SubscriberRegistryConsumer(handler, featureToggle);
    }

    @Test
    void appliesMessage_whenFeatureEnabled() {
        featureEnabled = true;

        consumer.accept(registeredMessage());

        assertThat(registry.isWatching("alice")).isTrue();
    }

    @Test
    void dropsMessage_whenFeatureDisabled() {
        featureEnabled = false;

        consumer.accept(registeredMessage());

        assertThat(registry.isWatching("alice")).isFalse();
        assertThat(registry.size()).isZero();
    }

    private static Message<SubscriberRegistryMessage> registeredMessage() {
        return new GenericMessage<>(SubscriberRegistryMessage.registered("alice", List.of("eng"), "rest-1", T0));
    }
}
