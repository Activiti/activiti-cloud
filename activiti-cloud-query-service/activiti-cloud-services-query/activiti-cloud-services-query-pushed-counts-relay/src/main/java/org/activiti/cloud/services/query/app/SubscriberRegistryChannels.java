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

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 * Channels for the subscriber-registry producer half of the relay. {@link #REGISTRY_PRODUCER} carries
 * this instance's presence (REGISTERED / UNREGISTERED / HEARTBEAT / SNAPSHOT); {@link #RESYNC_CONSUMER}
 * receives the fan-out registry traffic so the responder can reply to a consumer's RESYNC_REQUEST.
 *
 * <p>Names are deliberately distinct from the query-consumer's {@code subscriberRegistryProducer} /
 * {@code subscriberRegistryConsumer}, so a process combining query-rest with query-consumer keeps four
 * independent bindings; and the function bean that reads {@code RESYNC_CONSUMER} is named separately
 * again from the channel, as Spring Cloud Stream requires.
 */
public interface SubscriberRegistryChannels {
    String REGISTRY_PRODUCER = "pushedCountsRegistryProducer";

    String RESYNC_CONSUMER = "subscriberRegistryResyncConsumer";

    @OutputBinding(REGISTRY_PRODUCER)
    default MessageChannel pushedCountsRegistryProducer() {
        return MessageChannels.direct(REGISTRY_PRODUCER).getObject();
    }

    @InputBinding(RESYNC_CONSUMER)
    default SubscribableChannel subscriberRegistryResyncConsumer() {
        return MessageChannels.publishSubscribe(RESYNC_CONSUMER).getObject();
    }
}
