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

import java.time.Clock;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * On startup, asks every query-rest instance to replay its local registry so the consumer's
 * in-memory view is rebuilt after a restart. Broadcast only while the pushed-counts toggle is on;
 * the SNAPSHOT replies come back on the registry channel and are merged by the normal handler.
 */
public class SubscriberRegistryResyncRequester {

    private final MessageChannel registryProducer;
    private final FeatureToggle featureToggle;
    private final String sourceId;
    private final Clock clock;

    public SubscriberRegistryResyncRequester(
        MessageChannel registryProducer,
        FeatureToggle featureToggle,
        String sourceId,
        Clock clock
    ) {
        this.registryProducer = registryProducer;
        this.featureToggle = featureToggle;
        this.sourceId = sourceId;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requestResync() {
        if (featureToggle.isEnabled(QueryFeatureToggles.FEATURE_PUSHED_COUNTS)) {
            registryProducer.send(
                new GenericMessage<>(SubscriberRegistryMessage.resyncRequest(sourceId, clock.instant()))
            );
        }
    }
}
