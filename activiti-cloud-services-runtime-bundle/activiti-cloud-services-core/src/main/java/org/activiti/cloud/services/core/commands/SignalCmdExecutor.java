package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.RuntimeCommands;
import org.activiti.runtime.api.cmd.SendSignal;
import org.activiti.runtime.api.cmd.result.impl.SendSignalResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalCmdExecutor implements CommandExecutor<SendSignal> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public SignalCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                             MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return RuntimeCommands.SEND_SIGNAL.name();
    }

    @Override
    public void execute(SendSignal cmd) {
        processInstanceService.signal(cmd);
        SendSignalResultImpl cmdResult = new SendSignalResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
