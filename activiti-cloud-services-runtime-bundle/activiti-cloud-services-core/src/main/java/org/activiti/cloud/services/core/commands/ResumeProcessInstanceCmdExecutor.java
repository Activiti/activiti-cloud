package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.ResumeProcess;
import org.activiti.runtime.api.cmd.result.impl.ResumeProcessResultImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class ResumeProcessInstanceCmdExecutor implements CommandExecutor<ResumeProcess> {

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
        return ProcessCommands.RESUME_PROCESS.name();
    }

    @Override
    public void execute(ResumeProcess cmd) {
        processInstanceService.activate(cmd);
        ResumeProcessResultImpl cmdResult = new ResumeProcessResultImpl(cmd);
        commandResults.send(MessageBuilder.withPayload(cmdResult).build());
    }
}
