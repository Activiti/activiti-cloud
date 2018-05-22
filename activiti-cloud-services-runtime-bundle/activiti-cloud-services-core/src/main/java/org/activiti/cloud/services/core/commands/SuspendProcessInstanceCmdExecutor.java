package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.SuspendProcessInstanceResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SuspendProcessInstanceCmdExecutor implements CommandExecutor<SuspendProcessInstanceCmd> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SuspendProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                             MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return SuspendProcessInstanceCmd.class;
    }

    @Override
    public void execute(SuspendProcessInstanceCmd cmd) {
        processInstanceService.suspend(cmd);
        SuspendProcessInstanceResults cmdResult = new SuspendProcessInstanceResults(cmd.getId());
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
