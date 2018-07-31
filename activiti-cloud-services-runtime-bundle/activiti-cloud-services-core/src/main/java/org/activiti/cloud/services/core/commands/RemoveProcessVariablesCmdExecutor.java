package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.EmptyResult;
import org.activiti.runtime.api.model.payloads.RemoveProcessVariablesPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RemoveProcessVariablesCmdExecutor implements CommandExecutor<RemoveProcessVariablesPayload> {

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
        return RemoveProcessVariablesPayload.class.getName();
    }

    @Override
    public void execute(RemoveProcessVariablesPayload removeProcessVariablesPayload) {
        securityAwareProcessInstanceService.removeProcessVariables(removeProcessVariablesPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(removeProcessVariablesPayload)).build());
    }
}