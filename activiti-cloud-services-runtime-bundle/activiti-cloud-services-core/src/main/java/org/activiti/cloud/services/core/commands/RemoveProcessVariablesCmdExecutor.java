package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.results.RemoveProcessVariablesResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RemoveProcessVariablesCmdExecutor implements CommandExecutor<RemoveProcessVariablesCmd> {

    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public RemoveProcessVariablesCmdExecutor(SecurityAwareProcessInstanceService securityAwareProcessInstanceService,
                                             MessageChannel commandResults) {
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return RemoveProcessVariablesCmd.class;
    }

    @Override
    public void execute(RemoveProcessVariablesCmd cmd) {
        securityAwareProcessInstanceService.removeProcessVariables(cmd);
        RemoveProcessVariablesResults cmdResult = new RemoveProcessVariablesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}