package org.activiti.services.subscription.channel;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessEngineSignalChannels {

    String SIGNAL_CONSUMER = "signalConsumer";

    String SIGNAL_PRODUCER = "signalProducer";

    @Input(SIGNAL_CONSUMER)
    SubscribableChannel signalConsumer();

    @Output(SIGNAL_PRODUCER)
    MessageChannel signalProducer();
}
