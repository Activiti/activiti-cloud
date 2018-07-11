package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.SuspendProcess;
import org.activiti.runtime.api.cmd.result.impl.SuspendProcessResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SuspendProcessInstanceCmdExecutor implements CommandExecutor<SuspendProcess> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SuspendProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                             MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ProcessCommands.SUSPEND_PROCESS.name();
    }

    @Override
    public void execute(SuspendProcess cmd) {
        processInstanceService.suspend(cmd);
        SuspendProcessResultImpl cmdResult = new SuspendProcessResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
