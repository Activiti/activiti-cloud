package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.CompleteTask;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.result.impl.CompleteTaskResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class CompleteTaskCmdExecutor implements CommandExecutor<CompleteTask> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public CompleteTaskCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                   MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return TaskCommands.COMPLETE_TASK.name();
    }

    @Override
    public void execute(CompleteTask cmd) {
        securityAwareTaskService.completeTask(cmd);
        CompleteTaskResultImpl cmdResult = new CompleteTaskResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
