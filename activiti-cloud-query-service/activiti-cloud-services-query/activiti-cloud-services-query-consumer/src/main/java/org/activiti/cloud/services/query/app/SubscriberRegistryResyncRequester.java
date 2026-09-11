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
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * On startup, asks every query-rest instance to replay its local registry so the consumer's
 * in-memory view is rebuilt after a restart. Runs whenever the feature is wired (the startup
 * property); SNAPSHOT replies come back on the registry channel and are merged by the normal
 * handler. A rolling restart briefly multiplies this (every consumer asks, every instance replies),
 * but the union merge makes the duplicate snapshots harmless.
 *
 * <p>The broadcast is best-effort: a broker that is unreachable when the application becomes ready
 * must not abort startup, so a send failure is logged and swallowed. Normal registry traffic
 * (heartbeats and snapshots) rebuilds the view once the broker is back.
 */
public class SubscriberRegistryResyncRequester {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistryResyncRequester.class);

    private final MessageChannel registryProducer;
    private final String sourceId;
    private final Clock clock;

    public SubscriberRegistryResyncRequester(MessageChannel registryProducer, String sourceId, Clock clock) {
        this.registryProducer = registryProducer;
        this.sourceId = sourceId;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requestResync() {
        try {
            registryProducer.send(
                new GenericMessage<>(SubscriberRegistryMessage.resyncRequest(sourceId, clock.instant()))
            );
        } catch (RuntimeException e) {
            LOGGER.warn(
                "Could not broadcast subscriber registry resync request on startup; " +
                    "the registry will be rebuilt from normal traffic",
                e
            );
        }
    }
}
