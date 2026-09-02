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

import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;

/**
 * Applies each {@link SubscriberRegistryMessage} received on the registry fan-out channel to the
 * {@link ConsumerSubscriberRegistry}.
 */
public class SubscriberRegistryMessageHandler {

    private final ConsumerSubscriberRegistry registry;

    public SubscriberRegistryMessageHandler(ConsumerSubscriberRegistry registry) {
        this.registry = registry;
    }

    public void handle(SubscriberRegistryMessage message) {
        switch (message.type()) {
            case REGISTERED -> registry.register(
                message.userId(),
                message.groups(),
                message.sourceId(),
                message.sentAt()
            );
            case UNREGISTERED -> registry.unregister(message.userId(), message.sourceId());
            case HEARTBEAT -> registry.heartbeat(message.sourceId(), message.sentAt());
            case SNAPSHOT -> registry.applySnapshot(message.sourceId(), message.entries(), message.sentAt());
            // RESYNC_REQUEST is emitted by the consumer itself and carries no registry update.
            case RESYNC_REQUEST -> {}
        }
    }
}
