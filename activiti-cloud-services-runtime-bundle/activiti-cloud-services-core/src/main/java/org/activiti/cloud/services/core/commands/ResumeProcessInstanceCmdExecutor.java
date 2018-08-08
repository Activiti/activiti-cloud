package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.ProcessAdminRuntime;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.results.ProcessInstanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ResumeProcessInstanceCmdExecutor implements CommandExecutor<ResumeProcessPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private MessageChannel commandResults;

    @Autowired
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
