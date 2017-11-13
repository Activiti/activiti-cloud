package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.results.SetTaskVariablesResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetTaskVariablesCmdExecutor implements CommandExecutor<SetTaskVariablesCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public SetTaskVariablesCmdExecutor(ProcessEngineWrapper processEngine,
                                       MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SetTaskVariablesCmd.class;
    }

    @Override
    public void execute(SetTaskVariablesCmd cmd) {
        processEngine.setTaskVariables(cmd);
        SetTaskVariablesResults cmdResult = new SetTaskVariablesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
