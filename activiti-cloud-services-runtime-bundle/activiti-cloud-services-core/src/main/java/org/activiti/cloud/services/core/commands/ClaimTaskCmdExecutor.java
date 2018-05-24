package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.results.ClaimTaskResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCmdExecutor implements CommandExecutor<ClaimTaskCmd> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public ClaimTaskCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return ClaimTaskCmd.class;
    }

    @Override
    public void execute(ClaimTaskCmd cmd) {
        securityAwareTaskService.claimTask(cmd);
        ClaimTaskResults cmdResult = new ClaimTaskResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
