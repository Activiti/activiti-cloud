package org.activiti.cloud.common.messaging.config.test;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface TestBindingsChannels {

    String COMMAND_CONSUMER = "commandConsumer";

    String AUDIT_CONSUMER = "auditConsumer";

    String QUERY_CONSUMER = "queryConsumer";

    String COMMAND_RESULTS = "commandResults";

    String AUDIT_PRODUCER = "auditProducer";

    SubscribableChannel commandConsumer();

    SubscribableChannel queryConsumer();

    SubscribableChannel auditConsumer();

    MessageChannel commandResults();

    MessageChannel auditProducer();

}
