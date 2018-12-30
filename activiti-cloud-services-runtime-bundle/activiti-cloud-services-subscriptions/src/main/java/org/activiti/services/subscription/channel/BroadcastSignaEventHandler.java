package org.activiti.services.subscription.channel;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(ProcessEngineSignalChannels.class)
public class BroadcastSignaEventHandler {

    private final RuntimeService runtimeService;

    @Autowired
    public BroadcastSignaEventHandler(BinderAwareChannelResolver resolver,
                                      RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @StreamListener(ProcessEngineSignalChannels.SIGNAL_CONSUMER)
    public void receive(SignalPayload signalPayload) {
        if ((signalPayload.getVariables() == null) || (signalPayload.getVariables().isEmpty())) {
            runtimeService.signalEventReceived(signalPayload.getName());
        } else {
            runtimeService.signalEventReceived(signalPayload.getName(),
                                               signalPayload.getVariables());
        }
    }
}
