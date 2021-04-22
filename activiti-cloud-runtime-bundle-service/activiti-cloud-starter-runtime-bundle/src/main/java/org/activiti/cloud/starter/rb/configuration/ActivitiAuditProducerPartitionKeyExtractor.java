package org.activiti.cloud.starter.rb.configuration;

import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.messaging.Message;

import java.util.Optional;
import java.util.UUID;

public class ActivitiAuditProducerPartitionKeyExtractor implements PartitionKeyExtractorStrategy {

    @Override
    public Object extractKey(Message<?> message) {
        // Use processInstanceId header to route message between partitions or use random hash value if missing
        String rootProcessInstance = message.getHeaders()
                                            .get("rootProcessInstanceId",
                                                 String.class);

        return Optional.ofNullable(rootProcessInstance)
                       .orElse(UUID.randomUUID().toString());
    }

    PartitionHandler ph;
}
