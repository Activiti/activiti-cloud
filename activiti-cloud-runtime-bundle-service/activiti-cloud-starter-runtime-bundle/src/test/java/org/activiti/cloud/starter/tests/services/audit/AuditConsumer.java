package org.activiti.cloud.starter.tests.services.audit;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface AuditConsumer {

    String AUDIT_CONSUMER = "auditConsumer";

    @Input(AUDIT_CONSUMER)
    SubscribableChannel auditConsumer();
}
