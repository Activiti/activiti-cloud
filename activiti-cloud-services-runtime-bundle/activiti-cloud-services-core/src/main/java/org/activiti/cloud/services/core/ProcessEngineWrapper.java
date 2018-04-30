package org.activiti.cloud.services.core;

import java.util.List;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.CreateTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.api.commands.RemoveProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetProcessVariablesCmd;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.api.model.converter.ProcessInstanceConverter;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.activiti.cloud.services.core.pageable.PageableProcessInstanceService;
import org.activiti.cloud.services.core.pageable.PageableTaskService;
import org.activiti.cloud.services.events.listeners.MessageProducerActivitiEventListener;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProcessEngineWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineWrapper.class);

    private final ProcessInstanceConverter processInstanceConverter;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final TaskConverter taskConverter;
    private final PageableTaskService pageableTaskService;
    private final SecurityPoliciesApplicationService securityService;
    private final RepositoryService repositoryService;
    private final AuthenticationWrapper authenticationWrapper;
    private PageableProcessInstanceService pageableProcessInstanceService;

    @Autowired
    public ProcessEngineWrapper(ProcessInstanceConverter processInstanceConverter,
                                RuntimeService runtimeService,
                                PageableProcessInstanceService pageableProcessInstanceService,
                                TaskService taskService,
                                TaskConverter taskConverter,
                                PageableTaskService pageableTaskService,
                                MessageProducerActivitiEventListener listener,
                                SecurityPoliciesApplicationService securityService,
                                RepositoryService repositoryService,
                                AuthenticationWrapper authenticationWrapper) {
        this.processInstanceConverter = processInstanceConverter;
        this.runtimeService = runtimeService;
        this.pageableProcessInstanceService = pageableProcessInstanceService;
        this.taskService = taskService;
        this.taskConverter = taskConverter;
        this.pageableTaskService = pageableTaskService;
        this.runtimeService.addEventListener(listener);
        this.securityService = securityService;
        this.repositoryService = repositoryService;
        this.authenticationWrapper = authenticationWrapper;
    }

    public Page<ProcessInstance> getProcessInstances(Pageable pageable) {
        return pageableProcessInstanceService.getProcessInstances(pageable);
    }

    public Page<ProcessInstance> getAllProcessInstances(Pageable pageable) {
        return pageableProcessInstanceService.getAllProcessInstances(pageable);
    }

    public ProcessInstance startProcess(StartProcessInstanceCmd cmd) {

        String processDefinitionKey = null;
        if (cmd.getProcessDefinitionKey() != null) {
            long count = repositoryService.createProcessDefinitionQuery().processDefinitionKey(cmd.getProcessDefinitionKey()).count();
            if (count == 0) {
                throw new ActivitiObjectNotFoundException("Unable to find process definition for the given key:'" + cmd.getProcessDefinitionKey() + "'");
            }
            processDefinitionKey = cmd.getProcessDefinitionKey();
        } else {
            ProcessDefinition definition = repositoryService.getProcessDefinition(cmd.getProcessDefinitionId());
            if (definition == null) {
                throw new ActivitiObjectNotFoundException("Unable to find process definition for the given id:'" + cmd.getProcessDefinitionId() + "'");
            }
            processDefinitionKey = definition.getKey();
        }

        if (!securityService.canWrite(processDefinitionKey)) {
            LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processDefinitionKey);
            throw new ActivitiForbiddenException("Operation not permitted for " + processDefinitionKey);
        }

        ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder();
        if (cmd.getProcessDefinitionKey() != null) {
            builder.processDefinitionKey(cmd.getProcessDefinitionKey());
        } else {
            builder.processDefinitionId(cmd.getProcessDefinitionId());
        }
        builder.variables(cmd.getVariables());
        builder.businessKey(cmd.getBusinessKey());
        return processInstanceConverter.from(builder.start());
    }

    public void signal(SignalProcessInstancesCmd signalProcessInstancesCmd) {
        //TODO: plan is to restrict access to events using a new security policy on events
        // - that's another piece of work though so for now no security here

        runtimeService.signalEventReceived(signalProcessInstancesCmd.getName(),
                                           signalProcessInstancesCmd.getInputVariables());
    }

    public void suspend(SuspendProcessInstanceCmd suspendProcessInstanceCmd) {
        verifyCanWriteToProcessInstance(suspendProcessInstanceCmd.getProcessInstanceId());
        runtimeService.suspendProcessInstanceById(suspendProcessInstanceCmd.getProcessInstanceId());
    }

    private void verifyCanWriteToProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstanceById(processInstanceId);
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

    public void activate(ActivateProcessInstanceCmd activateProcessInstanceCmd) {
        verifyCanWriteToProcessInstance(activateProcessInstanceCmd.getProcessInstanceId());
        runtimeService.activateProcessInstanceById(activateProcessInstanceCmd.getProcessInstanceId());
    }

    public ProcessInstance getProcessInstanceById(String processInstanceId) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        query = query.processInstanceId(processInstanceId);
        org.activiti.engine.runtime.ProcessInstance processInstance = query.singleResult();
        return processInstanceConverter.from(processInstance);
    }

    public List<String> getActiveActivityIds(String executionId) {
        return runtimeService.getActiveActivityIds(executionId);
    }

    public Page<Task> getTasks(Pageable pageable) {
        return pageableTaskService.getTasks(pageable);
    }

    public Page<Task> getAllTasks(Pageable pageable) {
        return pageableTaskService.getAllTasks(pageable);
    }

    public Task getTaskById(String taskId) {
        org.activiti.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        return taskConverter.from(task);
    }

    public Task claimTask(ClaimTaskCmd claimTaskCmd) {
        taskService.claim(claimTaskCmd.getTaskId(),
                          claimTaskCmd.getAssignee());
        return taskConverter.from(taskService.createTaskQuery().taskId(claimTaskCmd.getTaskId()).singleResult());
    }

    public Task releaseTask(ReleaseTaskCmd releaseTaskCmd) {
        taskService.unclaim(releaseTaskCmd.getTaskId());
        return taskConverter.from(taskService.createTaskQuery().taskId(releaseTaskCmd.getTaskId()).singleResult());
    }

    public void completeTask(CompleteTaskCmd completeTaskCmd) {
        if (completeTaskCmd != null) {
            taskService.complete(completeTaskCmd.getTaskId(),
                                 completeTaskCmd.getOutputVariables());
        }
    }

    public void setTaskVariables(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariables(setTaskVariablesCmd.getTaskId(),
                                 setTaskVariablesCmd.getVariables());
    }

    public void setTaskVariablesLocal(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariablesLocal(setTaskVariablesCmd.getTaskId(),
                                      setTaskVariablesCmd.getVariables());
    }

    public void setProcessVariables(SetProcessVariablesCmd setProcessVariablesCmd) {
        ProcessInstance processInstance = getProcessInstanceById(setProcessVariablesCmd.getProcessId());
        verifyCanWriteToProcessInstance(processInstance.getId());
        runtimeService.setVariables(setProcessVariablesCmd.getProcessId(),
        setProcessVariablesCmd.getVariables());
    }

    public void removeProcessVariables(RemoveProcessVariablesCmd removeProcessVariablesCmd){
        ProcessInstance processInstance = getProcessInstanceById(removeProcessVariablesCmd.getProcessId());
        verifyCanWriteToProcessInstance(processInstance.getId());
        runtimeService.removeVariables(removeProcessVariablesCmd.getProcessId(),removeProcessVariablesCmd.getVariableNames());
    }

    /**
     * Delete task by id.
     * @param taskId the task id to delete
     */
    public void deleteTask(String taskId) {
        Task task = getTaskById(taskId);
        if (task == null) {
            throw new ActivitiObjectNotFoundException("Unable to find task for the given id: " + taskId);
        }

        checkWritePermissionsOnTask(task);

        taskService.deleteTask(taskId,
                               "Cancelled by " + authenticationWrapper.getAuthenticatedUserId());
    }

    public void checkWritePermissionsOnTask(Task task) {
        //TODO: to check the user write permissions on task
    }

    public Task createNewTask(CreateTaskCmd createTaskCmd) {
        final org.activiti.engine.task.Task task = taskService.newTask();
        task.setName(createTaskCmd.getName());
        task.setDescription(createTaskCmd.getDescription());
        task.setDueDate(createTaskCmd.getDueDate());
        if (createTaskCmd.getPriority() != null) {
            task.setPriority(createTaskCmd.getPriority());
        }
        taskService.saveTask(task);

        // see ACTIVITI#1854
        task.setAssignee(createTaskCmd.getAssignee() == null ? authenticationWrapper.getAuthenticatedUserId() : createTaskCmd.getAssignee());
        taskService.saveTask(task);

        return taskConverter.from(taskService.createTaskQuery().taskId(task.getId()).singleResult());
    }

    public void deleteProcessInstance(String processInstanceId) {
        verifyCanWriteToProcessInstance(processInstanceId);
        runtimeService.deleteProcessInstance(processInstanceId,
                                             "Cancelled by " + authenticationWrapper.getAuthenticatedUserId());
    }
}
