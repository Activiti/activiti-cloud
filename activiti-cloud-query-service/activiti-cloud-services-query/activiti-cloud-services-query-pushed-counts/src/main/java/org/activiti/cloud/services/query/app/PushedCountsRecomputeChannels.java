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
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

/**
 * This instance's own subscription to {@code queryEvents} (query-consumer's post-persistence
 * re-publish), independent of any other consumer of that topic. No consumer group: every instance
 * gets its own queue and a full copy of every batch.
 */
public interface PushedCountsRecomputeChannels {
    String QUERY_EVENTS_CONSUMER = "pushedCountsQueryEventsConsumer";

    @InputBinding(QUERY_EVENTS_CONSUMER)
    default SubscribableChannel pushedCountsQueryEventsConsumer() {
        return MessageChannels.publishSubscribe(QUERY_EVENTS_CONSUMER).getObject();
    }
}
