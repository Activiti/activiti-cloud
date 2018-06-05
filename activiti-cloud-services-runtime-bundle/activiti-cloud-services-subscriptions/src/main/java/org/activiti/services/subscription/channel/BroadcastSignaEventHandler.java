package org.activiti.services.subscription.channel;

import org.activiti.cloud.services.api.commands.SignalCmd;
import org.activiti.engine.RuntimeService;
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
    public void receive(SignalCmd signalCmd) {
        if ((signalCmd.getInputVariables() == null) || (signalCmd.getInputVariables().isEmpty())) {
            runtimeService.signalEventReceived(signalCmd.getName());
        } else {
            runtimeService.signalEventReceived(signalCmd.getName(), signalCmd.getInputVariables());
        }
    }

    public void broadcastSignal(SignalCmd signalCmd) {
        Message<SignalCmd> message = MessageBuilder.withPayload(signalCmd).build();
        resolver.resolveDestination("signalEvent").send(message);
    }
}
