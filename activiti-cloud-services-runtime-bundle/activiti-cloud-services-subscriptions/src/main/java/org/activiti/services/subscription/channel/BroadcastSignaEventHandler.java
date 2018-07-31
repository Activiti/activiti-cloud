package org.activiti.services.subscription.channel;

import org.activiti.engine.RuntimeService;
import org.activiti.runtime.api.model.payloads.SignalPayload;
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
    public void receive(SignalPayload signalPayload) {
        if ((signalPayload.getVariables() == null) || (signalPayload.getVariables().isEmpty())) {
            runtimeService.signalEventReceived(signalPayload.getName());
        } else {
            runtimeService.signalEventReceived(signalPayload.getName(),
                                               signalPayload.getVariables());
        }
    }

    public void broadcastSignal(SignalPayload signalPayload) {
        Message<SignalPayload> message = MessageBuilder.withPayload(signalPayload).build();
        resolver.resolveDestination("signalEvent").send(message);
    }
}
