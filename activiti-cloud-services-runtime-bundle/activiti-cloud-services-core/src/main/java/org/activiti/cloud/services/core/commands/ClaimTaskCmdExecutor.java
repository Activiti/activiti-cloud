package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.results.ClaimTaskResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCmdExecutor implements CommandExecutor<ClaimTaskCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public ClaimTaskCmdExecutor(ProcessEngineWrapper processEngine,
                                MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return ClaimTaskCmd.class;
    }

    @Override
    public void execute(ClaimTaskCmd cmd) {
        processEngine.claimTask(cmd);
        ClaimTaskResults cmdResult = new ClaimTaskResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
