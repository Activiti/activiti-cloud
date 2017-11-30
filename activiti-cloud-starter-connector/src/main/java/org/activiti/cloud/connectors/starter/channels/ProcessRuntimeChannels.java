package org.activiti.cloud.connectors.starter.channels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ProcessRuntimeChannels {
    String RUNTIME_CMD_PRODUCER = "runtimeCmdProducer";
    String RUNTIME_CMD_RESULTS = "runtimeCmdResults";

    @Output(RUNTIME_CMD_PRODUCER)
    MessageChannel runtimeCmdProducer();


    @Input(RUNTIME_CMD_RESULTS)
    SubscribableChannel runtimeCmdResults();

}
