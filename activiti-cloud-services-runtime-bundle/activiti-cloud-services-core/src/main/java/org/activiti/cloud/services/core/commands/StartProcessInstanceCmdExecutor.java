package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.StartProcessInstanceResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.model.ProcessInstance;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class StartProcessInstanceCmdExecutor implements CommandExecutor<StartProcessInstanceCmd> {

    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;
    private MessageChannel commandResults;

    public StartProcessInstanceCmdExecutor(SecurityAwareProcessInstanceService securityAwareProcessInstanceService,
                                           MessageChannel commandResults) {
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
        this.commandResults = commandResults;
    }

    @Override
    public Class getHandledType() {
        return StartProcessInstanceCmd.class;
    }

    @Override
    public void execute(StartProcessInstanceCmd cmd) {
        ProcessInstance processInstance = securityAwareProcessInstanceService.startProcess(cmd);
        if(processInstance != null) {
            StartProcessInstanceResults cmdResult = new StartProcessInstanceResults(cmd.getId(),
                                                                                    processInstance);
            commandResults.send(MessageBuilder.withPayload(cmdResult).build());
        }else{
            throw new IllegalStateException("Failed to start processInstance");
        }
    }
}
