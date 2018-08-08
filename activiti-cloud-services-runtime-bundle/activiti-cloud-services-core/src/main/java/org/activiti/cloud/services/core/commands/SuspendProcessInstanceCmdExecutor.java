package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.ProcessAdminRuntime;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.activiti.runtime.api.model.results.ProcessInstanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SuspendProcessInstanceCmdExecutor implements CommandExecutor<SuspendProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
    public SuspendProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime,
                                             MessageChannel commandResults) {
        this.processAdminRuntime = processAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return SuspendProcessPayload.class.getName();
    }

    @Override
    public void execute(SuspendProcessPayload suspendProcessPayload) {
        ProcessInstance processInstance = processAdminRuntime.suspend(suspendProcessPayload);
        ProcessInstanceResult result = new ProcessInstanceResult(suspendProcessPayload,
                                                                 processInstance);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
