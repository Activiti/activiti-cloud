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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class ConsistentHashRingPartitionedChannelKeySelectorTest {

    private ConsistentHashRing<Integer> consistentHashRing;
    private ConsistentHashRingPartitionedChannelKeySelector keySelector;

    @BeforeEach
    void setUp() {
        consistentHashRing = new ConsistentHashRing<>();
        consistentHashRing.addNode(0);
        consistentHashRing.addNode(1);
        consistentHashRing.addNode(2);
        keySelector = new ConsistentHashRingPartitionedChannelKeySelector(consistentHashRing);
    }

    @Test
    void shouldUseConsistentHashRingNodeWhenRootProcessInstanceIdHeaderExists() {
        String rootProcessInstanceId = "root-process-instance-id";
        Message<String> message = MessageBuilder
            .withPayload("payload")
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId)
            .build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(consistentHashRing.getNode(rootProcessInstanceId));
    }

    @Test
    void shouldUseDefaultNodeWhenRootProcessInstanceIdHeaderIsMissing() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(0);
    }

    @Test
    void shouldUseDefaultNodeWhenRootProcessInstanceIdHeaderIsNotString() {
        Message<String> message = MessageBuilder
            .withPayload("payload")
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, 123)
            .build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(0);
    }
}
