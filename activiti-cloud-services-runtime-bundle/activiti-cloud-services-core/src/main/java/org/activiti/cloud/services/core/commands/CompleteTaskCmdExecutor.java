package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.results.CompleteTaskResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class CompleteTaskCmdExecutor implements CommandExecutor<CompleteTaskCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public CompleteTaskCmdExecutor(ProcessEngineWrapper processEngine,
                                   MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return CompleteTaskCmd.class;
    }

    @Override
    public void execute(CompleteTaskCmd cmd) {
        processEngine.completeTask(cmd);
        CompleteTaskResults cmdResult = new CompleteTaskResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
