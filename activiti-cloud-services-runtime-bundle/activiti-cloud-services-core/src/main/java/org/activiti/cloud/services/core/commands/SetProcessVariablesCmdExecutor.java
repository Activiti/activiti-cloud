package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetProcessVariablesCmdExecutor implements CommandExecutor<SetProcessVariablesPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
    public SetProcessVariablesCmdExecutor(ProcessAdminRuntime processAdminRuntime,
                                          MessageChannel commandResults) {
        this.processAdminRuntime = processAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return SetProcessVariablesPayload.class.getName();
    }

    @Override
    public void execute(SetProcessVariablesPayload setProcessVariablesPayload) {
        processAdminRuntime.setVariables(setProcessVariablesPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(setProcessVariablesPayload)).build());
    }
}