package org.activiti.services.subscription;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.runtime.api.signal.SignalPayloadEventListener;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Overrides default SignalPayloadEventListener implementation to 
 * broadcast signals into Runtime Bundle instances via Cloud Stream
 */
@Component
public class SignalSender implements SignalPayloadEventListener {

    private final BinderAwareChannelResolver resolver;

    public SignalSender(BinderAwareChannelResolver resolver) {
        this.resolver = resolver;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendSignal(SignalPayload signalPayload) {
        Message<SignalPayload> message = MessageBuilder.withPayload(signalPayload).build();
        resolver.resolveDestination("signalEvent").send(message);
    }
}