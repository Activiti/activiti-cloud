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
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import reactor.core.publisher.Sinks;

class CountConsumerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private Sinks.Many<CountChangedMessage> sink;
    private boolean featureEnabled;
    private CountConsumer consumer;

    @BeforeEach
    void setUp() {
        sink = Sinks.many().multicast().onBackpressureBuffer();
        FeatureToggle featureToggle = name -> featureEnabled && QueryFeatureToggles.FEATURE_PUSHED_COUNTS.equals(name);
        consumer = new CountConsumer(sink, featureToggle);
    }

    @Test
    void emitsOntoTheSink_whenFeatureEnabled() {
        featureEnabled = true;
        List<CountChangedMessage> received = new ArrayList<>();
        sink.asFlux().subscribe(received::add);

        consumer.accept(countMessage());

        assertThat(received).containsExactly(new CountChangedMessage("assigned:alice", 3, NOW));
    }

    @Test
    void dropsMessage_whenFeatureDisabled() {
        featureEnabled = false;
        List<CountChangedMessage> received = new ArrayList<>();
        sink.asFlux().subscribe(received::add);

        consumer.accept(countMessage());

        assertThat(received).isEmpty();
    }

    private static Message<CountChangedMessage> countMessage() {
        return new GenericMessage<>(new CountChangedMessage("assigned:alice", 3, NOW));
    }
}
