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
package org.activiti.cloud.services.query.rest.subscriber;

import java.time.Clock;
import java.util.List;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Broadcasts this query-rest instance's local subscriber presence onto the shared registry
 * destination so the query-consumer can build one merged, cross-instance view. Translates the
 * in-process {@link SubscriberWentLiveEvent} / {@link SubscriberWentQuietEvent} into REGISTERED /
 * UNREGISTERED, emits a periodic HEARTBEAT so the consumer knows the instance is alive, and replays
 * its whole registry as a SNAPSHOT on startup. Every message carries this instance's stable
 * {@code sourceId}. Whether this runs at all is decided at startup by
 * {@code activiti.cloud.query.pushed-counts.enabled}; it never reads the runtime feature toggle,
 * which gates the count push (later steps), not presence tracking.
 */
public class SubscriberRegistryBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistryBroadcaster.class);

    private final StreamBridge streamBridge;
    private final SubscriberRegistry registry;
    private final String sourceId;
    private final String destination;
    private final Clock clock;

    public SubscriberRegistryBroadcaster(
        StreamBridge streamBridge,
        SubscriberRegistry registry,
        String sourceId,
        String destination,
        Clock clock
    ) {
        this.streamBridge = streamBridge;
        this.registry = registry;
        this.sourceId = sourceId;
        this.destination = destination;
        this.clock = clock;
    }

    @EventListener
    public void onWentLive(SubscriberWentLiveEvent event) {
        broadcast(
            SubscriberRegistryMessage.registered(event.userId(), List.copyOf(event.groups()), sourceId, event.at())
        );
    }

    @EventListener
    public void onWentQuiet(SubscriberWentQuietEvent event) {
        broadcast(SubscriberRegistryMessage.unregistered(event.userId(), sourceId, event.at()));
    }

    /** Announces this instance and replays whatever it already holds so a running consumer learns of it at once. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        broadcast(SubscriberRegistryMessage.snapshot(registry.snapshotEntries(), sourceId, clock.instant()));
    }

    @Scheduled(fixedRateString = "${activiti.cloud.query.pushed-counts.heartbeat-interval:PT1M}")
    public void heartbeat() {
        broadcast(SubscriberRegistryMessage.heartbeat(sourceId, clock.instant()));
    }

    private void broadcast(SubscriberRegistryMessage message) {
        LOGGER.debug("Broadcasting {} from instance {}", message.type(), sourceId);
        streamBridge.send(destination, message);
    }
}
