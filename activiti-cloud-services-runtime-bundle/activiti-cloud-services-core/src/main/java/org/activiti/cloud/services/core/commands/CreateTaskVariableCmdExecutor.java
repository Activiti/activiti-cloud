package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class CreateTaskVariableCmdExecutor implements CommandExecutor<CreateTaskVariablePayload> {

    private TaskAdminRuntime taskAdminRuntime;
    private MessageChannel commandResults;

    public CreateTaskVariableCmdExecutor(TaskAdminRuntime taskAdminRuntime,
                                       	 MessageChannel commandResults) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return CreateTaskVariablePayload.class.getName();
    }

    @Override
    public void execute(CreateTaskVariablePayload createTaskVariablePayload) {
        taskAdminRuntime.createVariable(createTaskVariablePayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(createTaskVariablePayload)).build());
    }
}
