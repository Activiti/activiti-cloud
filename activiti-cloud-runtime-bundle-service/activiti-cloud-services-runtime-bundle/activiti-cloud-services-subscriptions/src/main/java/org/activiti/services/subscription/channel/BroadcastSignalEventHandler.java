package org.activiti.services.subscription.channel;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.engine.RuntimeService;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;

public class BroadcastSignalEventHandler {

    private final RuntimeService runtimeService;

    public BroadcastSignalEventHandler(BinderAwareChannelResolver resolver,
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
