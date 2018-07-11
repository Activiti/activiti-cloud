package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.ClaimTask;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.result.impl.ClaimTaskResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCmdExecutor implements CommandExecutor<ClaimTask> {

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
        return TaskCommands.CLAIM_TASK.name();
    }

    @Override
    public void execute(ClaimTask cmd) {
        securityAwareTaskService.claimTask(cmd);
        ClaimTaskResultImpl cmdResult = new ClaimTaskResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
