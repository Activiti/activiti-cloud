package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SignalCmd;
import org.activiti.cloud.services.api.commands.results.SignalProcessInstancesResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalCmdExecutor implements CommandExecutor<SignalCmd> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SignalCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                             MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SignalCmd.class;
    }

    @Override
    public void execute(SignalCmd cmd) {
        processInstanceService.signal(cmd);
        SignalProcessInstancesResults cmdResult = new SignalProcessInstancesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
