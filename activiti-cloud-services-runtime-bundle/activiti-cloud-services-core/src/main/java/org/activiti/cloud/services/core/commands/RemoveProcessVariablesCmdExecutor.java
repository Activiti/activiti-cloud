package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class RemoveProcessVariablesCmdExecutor implements CommandExecutor<RemoveProcessVariablesPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    public RemoveProcessVariablesCmdExecutor(ProcessAdminRuntime processAdminRuntime,
                                             MessageChannel commandResults) {
        this.processAdminRuntime = processAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return RemoveProcessVariablesPayload.class.getName();
    }

    @Override
    public void execute(RemoveProcessVariablesPayload removeProcessVariablesPayload) {
        processAdminRuntime.removeVariables(removeProcessVariablesPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(removeProcessVariablesPayload)).build());
    }
}