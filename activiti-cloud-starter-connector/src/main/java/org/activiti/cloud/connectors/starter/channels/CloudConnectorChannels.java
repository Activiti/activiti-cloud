package org.activiti.cloud.connectors.starter.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface CloudConnectorChannels {
    String INTEGRATION_EVENT_CONSUMER = "integrationEventsConsumer";

    String INTEGRATION_RESULT_PRODUCER = "integrationResultsProducer";

    @Input(INTEGRATION_EVENT_CONSUMER)
    SubscribableChannel integrationEventConsumer();

    @Output(INTEGRATION_RESULT_PRODUCER)
    MessageChannel integrationResultProducer();
}
