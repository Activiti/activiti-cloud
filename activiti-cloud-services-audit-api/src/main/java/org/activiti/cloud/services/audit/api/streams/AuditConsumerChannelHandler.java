package org.activiti.cloud.services.audit.api.streams;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Headers;

import java.util.Map;

public interface AuditConsumerChannelHandler {

    @StreamListener(AuditConsumerChannels.AUDIT_CONSUMER)
    void receiveCloudRuntimeEvent(@Headers Map<String, Object> headers, CloudRuntimeEvent<?, ?>... events);
}
