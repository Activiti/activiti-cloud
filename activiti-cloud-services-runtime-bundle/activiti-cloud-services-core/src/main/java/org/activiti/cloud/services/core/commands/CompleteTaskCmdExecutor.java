package org.activiti.cloud.services.core.commands;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.results.TaskResult;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class CompleteTaskCmdExecutor implements CommandExecutor<CompleteTaskPayload> {

    private TaskAdminRuntime taskAdminRuntime;
    private MessageChannel commandResults;

    public CompleteTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime,
                                   MessageChannel commandResults) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return CompleteTaskPayload.class.getName();
    }

    @Override
    public void execute(CompleteTaskPayload completeTaskPayload) {
        Task task = taskAdminRuntime.complete(completeTaskPayload);
        TaskResult result = new TaskResult(completeTaskPayload,
                                           task);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
