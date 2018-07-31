package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.payloads.ClaimTaskPayload;
import org.activiti.runtime.api.model.results.TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCmdExecutor implements CommandExecutor<ClaimTaskPayload> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public ClaimTaskCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ClaimTaskPayload.class.getName();
    }

    @Override
    public void execute(ClaimTaskPayload claimTaskPayload) {
        Task task = securityAwareTaskService.claimTask(claimTaskPayload);
        TaskResult result = new TaskResult(claimTaskPayload, task);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
