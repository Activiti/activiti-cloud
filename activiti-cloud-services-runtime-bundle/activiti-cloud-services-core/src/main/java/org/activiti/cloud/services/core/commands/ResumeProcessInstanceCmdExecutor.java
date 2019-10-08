package org.activiti.cloud.services.core.commands;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.ResumeProcessPayload;
import org.activiti.api.process.model.results.ProcessInstanceResult;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public class ResumeProcessInstanceCmdExecutor implements CommandExecutor<ResumeProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    public ResumeProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime,
                                            MessageChannel commandResults) {
        this.processAdminRuntime = processAdminRuntime;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ResumeProcessPayload.class.getName();
    }

    @Override
    public void execute(ResumeProcessPayload resumeProcessPayload) {
        ProcessInstance processInstance = processAdminRuntime.resume(resumeProcessPayload);
        ProcessInstanceResult result = new ProcessInstanceResult(resumeProcessPayload,
                                                                 processInstance);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
