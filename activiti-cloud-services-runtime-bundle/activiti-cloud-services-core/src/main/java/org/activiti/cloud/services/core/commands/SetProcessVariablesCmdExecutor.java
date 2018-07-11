package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.SetProcessVariables;
import org.activiti.runtime.api.cmd.result.impl.SetProcessVariablesResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetProcessVariablesCmdExecutor implements CommandExecutor<SetProcessVariables> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SetProcessVariablesCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                          MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ProcessCommands.SET_PROCESS_VARIABLES.name();
    }

    @Override
    public void execute(SetProcessVariables cmd) {
        processInstanceService.setProcessVariables(cmd);
        SetProcessVariablesResultImpl cmdResult = new SetProcessVariablesResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }

}