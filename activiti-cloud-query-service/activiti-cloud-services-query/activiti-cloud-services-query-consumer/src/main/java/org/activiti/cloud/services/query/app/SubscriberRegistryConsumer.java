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
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Entry point for registry messages arriving on the broker. Applies each message to the registry;
 * one bad message is logged and skipped so it can't wedge the shared channel. Whether this is wired
 * at all is decided at startup by {@code activiti.cloud.query.pushed-counts.enabled}; registry
 * upkeep is intentionally independent of the runtime feature toggle, which gates the count push
 * (later steps), not presence tracking.
 */
public class SubscriberRegistryConsumer implements Consumer<Message<SubscriberRegistryMessage>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistryConsumer.class);

    private final SubscriberRegistryMessageHandler handler;

    public SubscriberRegistryConsumer(SubscriberRegistryMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void accept(Message<SubscriberRegistryMessage> message) {
        SubscriberRegistryMessage payload = message.getPayload();
        try {
            handler.handle(payload);
        } catch (RuntimeException e) {
            // One bad message must not wedge the shared channel; payload stays at DEBUG to keep subscriber ids out of normal logs.
            LOGGER.warn("Skipping a subscriber registry message that failed to apply", e);
            LOGGER.debug("Failed subscriber registry payload: {}", payload);
        }
    }
}
