package org.activiti.cloud.conf;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldUseDefaultPartitionWhenHeaderIsMissing() {
        Message<String> message = MessageBuilder.withPayload("payload").build();

        Object selectedKey = keySelector.apply(message);

        assertThat(selectedKey).isEqualTo(0);
    }
}
