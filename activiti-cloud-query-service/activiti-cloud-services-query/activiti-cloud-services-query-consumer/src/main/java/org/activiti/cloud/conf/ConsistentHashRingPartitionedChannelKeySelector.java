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

package org.activiti.cloud.conf;

import java.util.Optional;
import org.springframework.messaging.Message;

public class ConsistentHashRingPartitionedChannelKeySelector implements QueryConsumerPartitionedChannelKeySelector {

    private static final Integer DEFAULT_NODE = 0;

    private final ConsistentHashRing<Integer> consistentHashRing;

    public ConsistentHashRingPartitionedChannelKeySelector(ConsistentHashRing<Integer> consistentHashRing) {
        this.consistentHashRing = consistentHashRing;
    }

    @Override
    public Object apply(Message<?> message) {
        return Optional
            .ofNullable(message.getHeaders().get(ROOT_PROCESS_INSTANCE_ID))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(consistentHashRing::getNode)
            .orElse(DEFAULT_NODE);
    }
}
