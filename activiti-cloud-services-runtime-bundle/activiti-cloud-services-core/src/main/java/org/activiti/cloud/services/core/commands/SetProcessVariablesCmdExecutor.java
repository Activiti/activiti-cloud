package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.results.SetProcessVariablesResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetProcessVariablesCmdExecutor implements CommandExecutor<SetProcessVariablesCmd> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SetProcessVariablesCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                          MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SetProcessVariablesCmd.class;
    }

    @Override
    public void execute(SetProcessVariablesCmd cmd) {
        processInstanceService.setProcessVariables(cmd);
        SetProcessVariablesResults cmdResult = new SetProcessVariablesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}