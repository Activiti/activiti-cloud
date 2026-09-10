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
import java.util.function.Consumer;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

/**
 * Replies to the consumer's RESYNC_REQUEST with a SNAPSHOT of this instance's local registry, so a
 * restarted consumer rebuilds its merged view without waiting for users to reconnect. The registry
 * destination is a fan-out, so this also receives peers' messages and its own echoes; it acts only
 * on RESYNC_REQUEST and ignores anything it sent itself. Presence work is not gated by the runtime
 * feature toggle - the startup property decides whether this bean exists at all.
 */
public class SubscriberResyncResponder implements Consumer<Message<SubscriberRegistryMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberResyncResponder.class);

    private final SubscriberRegistry registry;
    private final StreamBridge streamBridge;
    private final String sourceId;
    private final String destination;
    private final Clock clock;

    public SubscriberResyncResponder(
        SubscriberRegistry registry,
        StreamBridge streamBridge,
        String sourceId,
        String destination,
        Clock clock
    ) {
        this.registry = registry;
        this.streamBridge = streamBridge;
        this.sourceId = sourceId;
        this.destination = destination;
        this.clock = clock;
    }

    @Override
    public void accept(Message<SubscriberRegistryMessage> message) {
        SubscriberRegistryMessage request = message.getPayload();
        if (request.type() != RegistryMessageType.RESYNC_REQUEST || sourceId.equals(request.sourceId())) {
            return;
        }
        LOGGER.debug("Replying with a registry snapshot to a resync request from {}", request.sourceId());
        streamBridge.send(
            destination,
            SubscriberRegistryMessage.snapshot(registry.snapshotEntries(), sourceId, clock.instant())
        );
    }
}
