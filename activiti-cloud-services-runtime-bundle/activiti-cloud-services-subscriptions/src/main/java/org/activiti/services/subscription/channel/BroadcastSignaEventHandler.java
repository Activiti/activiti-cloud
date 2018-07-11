package org.activiti.services.subscription.channel;

import org.activiti.engine.RuntimeService;
import org.activiti.runtime.api.cmd.SendSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ProcessEngineSignalChannels.class)
public class BroadcastSignaEventHandler {

    @Autowired
    private BinderAwareChannelResolver resolver;

    @Autowired
    private RuntimeService runtimeService;

    @StreamListener(ProcessEngineSignalChannels.SIGNAL_CONSUMER)
    public void receive(SendSignal signalCmd) {
        if ((signalCmd.getInputVariables() == null) || (signalCmd.getInputVariables().isEmpty())) {
            runtimeService.signalEventReceived(signalCmd.getName());
        } else {
            runtimeService.signalEventReceived(signalCmd.getName(),
                                               signalCmd.getInputVariables());
        }
    }

    public void broadcastSignal(SendSignal signalCmd) {
        Message<SendSignal> message = MessageBuilder.withPayload(signalCmd).build();
        resolver.resolveDestination("signalEvent").send(message);
    }
}
