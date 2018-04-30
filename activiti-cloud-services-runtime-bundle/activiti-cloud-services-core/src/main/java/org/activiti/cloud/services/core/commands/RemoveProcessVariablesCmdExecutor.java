package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.results.RemoveProcessVariablesResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RemoveProcessVariablesCmdExecutor implements CommandExecutor<RemoveProcessVariablesCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public RemoveProcessVariablesCmdExecutor(ProcessEngineWrapper processEngine,
                                             MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return RemoveProcessVariablesCmd.class;
    }

    @Override
    public void execute(RemoveProcessVariablesCmd cmd) {
        processEngine.removeProcessVariables(cmd);
        RemoveProcessVariablesResults cmdResult = new RemoveProcessVariablesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}