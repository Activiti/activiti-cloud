package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class UpdateTaskVariableCmdExecutor implements CommandExecutor<UpdateTaskVariablePayload> {

    private TaskAdminRuntime taskAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
    public UpdateTaskVariableCmdExecutor(TaskAdminRuntime taskAdminRuntime,
                                       	 MessageChannel commandResults) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return UpdateTaskVariablePayload.class.getName();
    }

    @Override
    public void execute(UpdateTaskVariablePayload updateTaskVariablePayload) {
        taskAdminRuntime.updateVariable(updateTaskVariablePayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(updateTaskVariablePayload)).build());
    }
}
