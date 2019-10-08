package org.activiti.cloud.services.core.commands;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.process.model.results.ProcessInstanceResult;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class SuspendProcessInstanceCmdExecutor implements CommandExecutor<SuspendProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

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
