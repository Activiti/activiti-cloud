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

import java.util.function.Consumer;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;

/**
 * Feature-gated entry point for pushed counts arriving on the broker, relaying each one into the
 * local {@link Sinks.Many} that feeds every subscription this instance is serving. While the
 * pushed-counts toggle is off the message is dropped, so the binding can stay in place idle and the
 * feature can be switched on at runtime without a redeploy.
 */
public class CountConsumer implements Consumer<Message<CountChangedMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountConsumer.class);

    private final Sinks.Many<CountChangedMessage> pushedCountsSink;
    private final FeatureToggle featureToggle;

    public CountConsumer(Sinks.Many<CountChangedMessage> pushedCountsSink, FeatureToggle featureToggle) {
        this.pushedCountsSink = pushedCountsSink;
        this.featureToggle = featureToggle;
    }

    @Override
    public void accept(Message<CountChangedMessage> message) {
        if (!featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            return;
        }
        CountChangedMessage payload = message.getPayload();
        Sinks.EmitResult result = pushedCountsSink.tryEmitNext(payload);
        if (result.isFailure()) {
            LOGGER.warn("Failed to emit pushed count onto the local sink: {}", result);
        }
    }
}
