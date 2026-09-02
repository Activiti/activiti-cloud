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
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.springframework.messaging.Message;

/**
 * Feature-gated entry point for registry messages arriving on the broker. While the pushed-counts
 * toggle is off the message is dropped, so the binding can stay in place with the registry idle and
 * the feature can be switched on at runtime without a redeploy.
 */
public class SubscriberRegistryConsumer implements Consumer<Message<SubscriberRegistryMessage>> {

    private final SubscriberRegistryMessageHandler handler;
    private final FeatureToggle featureToggle;

    public SubscriberRegistryConsumer(SubscriberRegistryMessageHandler handler, FeatureToggle featureToggle) {
        this.handler = handler;
        this.featureToggle = featureToggle;
    }

    @Override
    public void accept(Message<SubscriberRegistryMessage> message) {
        if (featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            handler.handle(message.getPayload());
        }
    }
}
