package org.activiti.cloud.examples.connectors;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

interface RestConnectorChannels {
    String REST_CONNECTOR = "restConnector";

    @InputBinding(REST_CONNECTOR)
    default SubscribableChannel restConnector() {
        return MessageChannels.publishSubscribe(REST_CONNECTOR).getObject();
    }
}
