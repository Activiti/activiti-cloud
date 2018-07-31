package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.activiti.runtime.api.model.results.ProcessInstanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SuspendProcessInstanceCmdExecutor implements CommandExecutor<SuspendProcessPayload> {

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
        return SuspendProcessPayload.class.getName();
    }

    @Override
    public void execute(SuspendProcessPayload suspendProcessPayload) {
        ProcessInstance processInstance = processInstanceService.suspend(suspendProcessPayload);
        ProcessInstanceResult result = new ProcessInstanceResult(suspendProcessPayload,
                                                    processInstance);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
