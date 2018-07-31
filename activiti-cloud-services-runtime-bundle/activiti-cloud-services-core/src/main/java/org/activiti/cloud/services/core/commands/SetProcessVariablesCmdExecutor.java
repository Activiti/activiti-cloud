package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.EmptyResult;
import org.activiti.runtime.api.model.payloads.SetProcessVariablesPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetProcessVariablesCmdExecutor implements CommandExecutor<SetProcessVariablesPayload> {

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
        return SetProcessVariablesPayload.class.getName();
    }

    @Override
    public void execute(SetProcessVariablesPayload setProcessVariablesPayload) {
        processInstanceService.setProcessVariables(setProcessVariablesPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(setProcessVariablesPayload)).build());
    }
}