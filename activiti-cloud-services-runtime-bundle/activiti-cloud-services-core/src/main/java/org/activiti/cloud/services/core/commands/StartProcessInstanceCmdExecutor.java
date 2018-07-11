package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.StartProcess;
import org.activiti.runtime.api.cmd.result.impl.StartProcessResultImpl;
import org.activiti.runtime.api.model.ProcessInstance;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class StartProcessInstanceCmdExecutor implements CommandExecutor<StartProcess> {

    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;
    private MessageChannel commandResults;

    public StartProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService securityAwareProcessInstanceService,
                                           MessageChannel commandResults) {
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public String getHandledType() {
        return ProcessCommands.START_PROCESS.name();
    }

    @Override
    public void execute(StartProcess cmd) {
        ProcessInstance processInstance = securityAwareProcessInstanceService.startProcess(cmd);
        if(processInstance != null) {
            StartProcessResultImpl cmdResult = new StartProcessResultImpl(cmd,
                                                                          processInstance);
            commandResults.send(MessageBuilder.withPayload(cmdResult).build());
        }else{
            throw new IllegalStateException("Failed to start processInstance");
        }
    }
}
