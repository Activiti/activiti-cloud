package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.EmptyResult;
import org.activiti.runtime.api.TaskAdminRuntime;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetTaskVariablesCmdExecutor implements CommandExecutor<SetTaskVariablesPayload> {

    private TaskAdminRuntime taskAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
    public SetTaskVariablesCmdExecutor(TaskAdminRuntime taskAdminRuntime,
                                       MessageChannel commandResults) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return SetTaskVariablesPayload.class.getName();
    }

    @Override
    public void execute(SetTaskVariablesPayload setTaskVariablesPayload) {
        taskAdminRuntime.setVariables(setTaskVariablesPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(setTaskVariablesPayload)).build());
    }
}
