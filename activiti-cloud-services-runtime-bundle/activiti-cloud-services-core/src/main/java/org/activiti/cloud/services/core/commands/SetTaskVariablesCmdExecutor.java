package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.SetTaskVariables;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.result.impl.SetTaskVariablesResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SetTaskVariablesCmdExecutor implements CommandExecutor<SetTaskVariables> {

    private SecurityAwareTaskService securityAwareTaskService;
    private MessageChannel commandResults;

    @Autowired
    public SetTaskVariablesCmdExecutor(SecurityAwareTaskService securityAwareTaskService,
                                       MessageChannel commandResults) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return TaskCommands.SET_TASK_VARIABLES.name();
    }

    @Override
    public void execute(SetTaskVariables cmd) {
        securityAwareTaskService.setTaskVariables(cmd);
        SetTaskVariablesResultImpl cmdResult = new SetTaskVariablesResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
