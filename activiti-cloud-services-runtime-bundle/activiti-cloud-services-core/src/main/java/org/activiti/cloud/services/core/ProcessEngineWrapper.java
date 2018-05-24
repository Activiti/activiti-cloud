package org.activiti.cloud.services.core;

import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.cloud.services.events.listeners.MessageProducerActivitiEventListener;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessEngineWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineWrapper.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final SecurityPoliciesApplicationService securityService;
    private final RepositoryService repositoryService;
    private final AuthenticationWrapper authenticationWrapper;
    private final SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @Autowired
    public ProcessEngineWrapper(RuntimeService runtimeService,
                                SecurityAwareProcessInstanceService securityAwareProcessInstanceService,
                                TaskService taskService,
                                MessageProducerActivitiEventListener listener,
                                SecurityPoliciesApplicationService securityService,
                                RepositoryService repositoryService,
                                AuthenticationWrapper authenticationWrapper) {
        this.runtimeService = runtimeService;
        this.securityAwareProcessInstanceService = securityAwareProcessInstanceService;
        this.taskService = taskService;
        this.runtimeService.addEventListener(listener);
        this.securityService = securityService;
        this.repositoryService = repositoryService;
        this.authenticationWrapper = authenticationWrapper;
    }

    private void verifyCanWriteToProcessInstance(String processInstanceId) {
        org.activiti.runtime.api.model.ProcessInstance processInstance = getProcessInstanceById(processInstanceId);
        if (processInstance == null) {
            throw new ActivitiException("Unable to find process instance for the given id: " + processInstanceId);
        }

        ProcessDefinition processDefinition =
                repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
        if (processDefinition == null) {
            throw new ActivitiException("Unable to find process definition for the given id: " + processInstance.getProcessDefinitionId());
        }

        if (!securityService.canWrite(processDefinition.getKey())) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinition.getKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinition.getKey());
        }
    }

    public org.activiti.runtime.api.model.ProcessInstance getProcessInstanceById(String processInstanceId) {
        return securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstanceId);
    }

    public void setTaskVariables(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariables(setTaskVariablesCmd.getTaskId(),
                                 setTaskVariablesCmd.getVariables());
    }

    public void setTaskVariablesLocal(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariablesLocal(setTaskVariablesCmd.getTaskId(),
                                      setTaskVariablesCmd.getVariables());
    }

    public void removeProcessVariables(RemoveProcessVariablesCmd removeProcessVariablesCmd) {
        org.activiti.runtime.api.model.ProcessInstance processInstance = getProcessInstanceById(removeProcessVariablesCmd.getProcessId());
        verifyCanWriteToProcessInstance(processInstance.getId());
        runtimeService.removeVariables(removeProcessVariablesCmd.getProcessId(),
                                       removeProcessVariablesCmd.getVariableNames());
    }

}
