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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class UUIDConsumerPartitionedChannelKeySelectorTest {

    private static final int TOTAL_PARTITIONS = 32;

    private final UUIDConsumerPartitionedChannelKeySelector keySelector = new UUIDConsumerPartitionedChannelKeySelector(
        TOTAL_PARTITIONS
    );

    @Test
    void shouldUseRootProcessInstanceIdHeaderWhenPresent() {
        String rootProcessInstanceId = UUID.randomUUID().toString();
        Message<String> message = MessageBuilder
            .withPayload("payload")
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId)
            .build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isIn(IntStream.range(0, TOTAL_PARTITIONS).boxed().toList());
    }

    @Test
    void shouldResolveToPositivePartitionKey() {
        String rootProcessInstanceId = "8650f557-9ff2-4319-8481-8ae64a99315a";
        Message<String> message = MessageBuilder
            .withPayload("payload")
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, rootProcessInstanceId)
            .build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(25);
    }

    @Test
    void shouldUseDefaultPartitionWhenHeaderIsMissing() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(0);
    }

    @Test
    void shouldUseDefaultPartitionWhenInvalidUUID() {
        Message<String> message = MessageBuilder
            .withPayload("payload")
            .setHeader(QueryConsumerPartitionedChannelKeySelector.ROOT_PROCESS_INSTANCE_ID, "foobar")
            .build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(0);
    }

    @Test
    void shouldRejectNegativePartitionCount() {
        assertThatThrownBy(() -> new UUIDConsumerPartitionedChannelKeySelector(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
