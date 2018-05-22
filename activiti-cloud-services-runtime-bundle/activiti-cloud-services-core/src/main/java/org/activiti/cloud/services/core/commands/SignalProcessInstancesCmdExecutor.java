package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.results.SignalProcessInstancesResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalProcessInstancesCmdExecutor implements CommandExecutor<SignalProcessInstancesCmd> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SignalProcessInstancesCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                             MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SignalProcessInstancesCmd.class;
    }

    @Override
    public void execute(SignalProcessInstancesCmd cmd) {
        processInstanceService.signal(cmd);
        SignalProcessInstancesResults cmdResult = new SignalProcessInstancesResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
