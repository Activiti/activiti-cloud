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

public interface QueryConsumerChannels {
    String QUERY_CONSUMER = "queryConsumer";

    String QUERY_EVENTS_PRODUCER = "queryEventsProducer";

    String SUBSCRIBER_REGISTRY_CONSUMER = "subscriberRegistryConsumer";

    String SUBSCRIBER_REGISTRY_PRODUCER = "subscriberRegistryProducer";

    String COUNT_PRODUCER = "countProducer";

    @InputBinding(QUERY_CONSUMER)
    default SubscribableChannel queryConsumer() {
        return MessageChannels.publishSubscribe(QUERY_CONSUMER).getObject();
    }

    @OutputBinding(QUERY_EVENTS_PRODUCER)
    default MessageChannel queryEventsProducer() {
        return MessageChannels.direct(QUERY_EVENTS_PRODUCER).getObject();
    }

    @InputBinding(SUBSCRIBER_REGISTRY_CONSUMER)
    default SubscribableChannel subscriberRegistryConsumer() {
        return MessageChannels.publishSubscribe(SUBSCRIBER_REGISTRY_CONSUMER).getObject();
    }

    @OutputBinding(SUBSCRIBER_REGISTRY_PRODUCER)
    default MessageChannel subscriberRegistryProducer() {
        return MessageChannels.direct(SUBSCRIBER_REGISTRY_PRODUCER).getObject();
    }

    @OutputBinding(COUNT_PRODUCER)
    default MessageChannel countProducer() {
        return MessageChannels.direct(COUNT_PRODUCER).getObject();
    }
}
