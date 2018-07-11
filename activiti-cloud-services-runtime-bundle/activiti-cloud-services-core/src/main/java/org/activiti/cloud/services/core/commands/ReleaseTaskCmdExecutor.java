package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.ReleaseTask;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.result.impl.ReleaseTaskResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ReleaseTaskCmdExecutor implements CommandExecutor<ReleaseTask> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public ReleaseTaskCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                  MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return TaskCommands.RELEASE_TASK.name();
    }

    @Override
    public void execute(ReleaseTask cmd) {
        securityAwareTaskService.releaseTask(cmd);
        ReleaseTaskResultImpl cmdResult = new ReleaseTaskResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
