package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalCmdExecutor implements CommandExecutor<SignalPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
    public SignalCmdExecutor(ProcessAdminRuntime processAdminRuntime,
                             MessageChannel commandResults) {
        this.processAdminRuntime = processAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return SignalPayload.class.getName();
    }

    @Override
    public void execute(SignalPayload signalPayload) {
        processAdminRuntime.signal(signalPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(signalPayload)).build());
    }
}
