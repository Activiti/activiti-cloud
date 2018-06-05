package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.results.SignalProcessInstancesResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.SignalCmd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalProcessInstancesCmdExecutor implements CommandExecutor<SignalCmd> {

    private ProcessEngineWrapper processEngine;
    private MessageChannel commandResults;

    @Autowired
    public SignalProcessInstancesCmdExecutor(ProcessEngineWrapper processEngine,
                                             MessageChannel commandResults) {
        this.processEngine = processEngine;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SignalCmd.class;
    }

    @Override
    public void execute(SignalCmd cmd) {
        processEngine.signal(cmd);
        SignalProcessInstancesResults cmdResult = new SignalProcessInstancesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
