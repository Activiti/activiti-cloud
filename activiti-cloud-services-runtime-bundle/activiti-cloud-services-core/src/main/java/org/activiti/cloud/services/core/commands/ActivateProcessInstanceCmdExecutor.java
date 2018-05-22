package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.ActivateProcessInstanceResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActivateProcessInstanceCmdExecutor implements CommandExecutor<ActivateProcessInstanceCmd> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public ActivateProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                              MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return ActivateProcessInstanceCmd.class;
    }

    @Override
    public void execute(ActivateProcessInstanceCmd cmd) {
        processInstanceService.activate(cmd);
        ActivateProcessInstanceResults cmdResult = new ActivateProcessInstanceResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
