package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.EmptyResult;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalCmdExecutor implements CommandExecutor<SignalPayload> {

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
        return SignalPayload.class.getName();
    }

    @Override
    public void execute(SignalPayload signalPayload) {
        processInstanceService.signal(signalPayload);
        commandResults.send(MessageBuilder.withPayload(new EmptyResult(signalPayload)).build());
    }
}
