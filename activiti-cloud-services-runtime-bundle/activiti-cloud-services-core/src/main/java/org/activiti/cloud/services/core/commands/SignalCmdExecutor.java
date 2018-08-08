package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.EmptyResult;
import org.activiti.runtime.api.ProcessAdminRuntime;
import org.activiti.runtime.api.model.payloads.SignalPayload;
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
