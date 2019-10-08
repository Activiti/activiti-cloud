package org.activiti.cloud.services.core.commands;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.model.results.TaskResult;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class ClaimTaskCmdExecutor implements CommandExecutor<ClaimTaskPayload> {

    private TaskAdminRuntime taskAdminRuntime;
    private MessageChannel commandResults;

    public ClaimTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime,
                                MessageChannel commandResults) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ClaimTaskPayload.class.getName();
    }

    @Override
    public void execute(ClaimTaskPayload claimTaskPayload) {
        Task task = taskAdminRuntime.claim(claimTaskPayload);
        TaskResult result = new TaskResult(claimTaskPayload, task);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
