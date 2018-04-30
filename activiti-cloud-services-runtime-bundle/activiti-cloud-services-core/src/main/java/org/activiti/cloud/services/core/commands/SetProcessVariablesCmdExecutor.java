package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.results.SetProcessVariablesResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetProcessVariablesCmdExecutor implements CommandExecutor<SetProcessVariablesCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public SetProcessVariablesCmdExecutor(ProcessEngineWrapper processEngine,
                                          MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SetProcessVariablesCmd.class;
    }

    @Override
    public void execute(SetProcessVariablesCmd cmd) {
        processEngine.setProcessVariables(cmd);
        SetProcessVariablesResults cmdResult = new SetProcessVariablesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}