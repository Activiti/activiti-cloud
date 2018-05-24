package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.results.CompleteTaskResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class CompleteTaskCmdExecutor implements CommandExecutor<CompleteTaskCmd> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public CompleteTaskCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                   MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return CompleteTaskCmd.class;
    }

    @Override
    public void execute(CompleteTaskCmd cmd) {
        securityAwareTaskService.completeTask(cmd);
        CompleteTaskResults cmdResult = new CompleteTaskResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
