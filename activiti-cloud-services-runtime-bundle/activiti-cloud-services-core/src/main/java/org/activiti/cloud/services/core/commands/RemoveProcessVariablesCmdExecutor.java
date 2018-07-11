package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.RemoveProcessVariables;
import org.activiti.runtime.api.cmd.result.impl.RemoveProcessVariablesResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RemoveProcessVariablesCmdExecutor implements CommandExecutor<RemoveProcessVariables> {

    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public RemoveProcessVariablesCmdExecutor(SecurityAwareProcessInstanceService securityAwareProcessInstanceService,
                                             MessageChannel commandResults) {
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ProcessCommands.REMOVE_PROCESS_VARIABLES.name();
    }

    @Override
    public void execute(RemoveProcessVariables cmd) {
        securityAwareProcessInstanceService.removeProcessVariables(cmd);
        RemoveProcessVariablesResultImpl cmdResult = new RemoveProcessVariablesResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}