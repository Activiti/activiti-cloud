package org.activiti.services.subscription;

import org.activiti.cloud.services.api.commands.SignalCmd;
import org.activiti.services.subscription.channel.BroadcastSignaEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SignalSender {

    private final BroadcastSignaEventHandler eventHandler;

    public SignalSender(BroadcastSignaEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendSignal(SignalCmd signalCmd) {
        eventHandler.broadcastSignal(signalCmd);
    }
}