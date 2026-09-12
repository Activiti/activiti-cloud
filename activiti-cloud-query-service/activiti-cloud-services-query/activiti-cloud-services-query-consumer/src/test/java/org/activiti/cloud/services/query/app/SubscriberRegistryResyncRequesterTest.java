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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

class SubscriberRegistryResyncRequesterTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private List<Message<?>> sent;
    private boolean featureEnabled;
    private SubscriberRegistryResyncRequester requester;

    @BeforeEach
    void setUp() {
        sent = new ArrayList<>();
        MessageChannel registryProducer = (message, timeout) -> sent.add(message);
        FeatureToggle featureToggle = name -> featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        requester = new SubscriberRegistryResyncRequester(
            registryProducer,
            featureToggle,
            "consumer-1",
            Clock.fixed(T0, ZoneOffset.UTC)
        );
    }

    @Test
    void broadcastsResyncRequest_whenFeatureEnabled() {
        featureEnabled = true;

        requester.requestResync();

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getPayload()).isInstanceOf(SubscriberRegistryMessage.class);
        SubscriberRegistryMessage message = (SubscriberRegistryMessage) sent.get(0).getPayload();
        assertThat(message.type()).isEqualTo(RegistryMessageType.RESYNC_REQUEST);
        assertThat(message.sourceId()).isEqualTo("consumer-1");
        assertThat(message.sentAt()).isEqualTo(T0);
    }

    @Test
    void broadcastsNothing_whenFeatureDisabled() {
        featureEnabled = false;

        requester.requestResync();

        assertThat(sent).isEmpty();
    }
}
