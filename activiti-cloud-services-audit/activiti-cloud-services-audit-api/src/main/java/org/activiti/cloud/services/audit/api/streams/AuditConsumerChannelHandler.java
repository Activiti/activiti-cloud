package org.activiti.cloud.services.audit.api.streams;

import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannels;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.springframework.cloud.stream.annotation.StreamListener;

public interface AuditConsumerChannelHandler {

    @StreamListener(AuditConsumerChannels.AUDIT_CONSUMER)
    void receiveCloudRuntimeEvent(CloudRuntimeEvent<?, ?>... events);
}
