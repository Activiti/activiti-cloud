package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.results.ProcessInstanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ResumeProcessInstanceCmdExecutor implements CommandExecutor<ResumeProcessPayload> {

    private SecurityAwareProcessInstanceService processInstanceService;
    private MessageChannel commandResults;

    @Autowired
    public ResumeProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService processInstanceService,
                                            MessageChannel commandResults) {
        this.processInstanceService = processInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ResumeProcessPayload.class.getName();
    }

    @Override
    public void execute(ResumeProcessPayload resumeProcessPayload) {
        ProcessInstance processInstance = processInstanceService.activate(resumeProcessPayload);
        ProcessInstanceResult result = new ProcessInstanceResult(resumeProcessPayload,
                                                    processInstance);
        commandResults.send(MessageBuilder.withPayload(result).build());
    }
}
