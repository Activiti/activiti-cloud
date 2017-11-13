package org.activiti.cloud.services.core;

import java.util.List;
import java.util.Map;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
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
import org.activiti.cloud.services.events.MessageProducerActivitiEventListener;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProcessEngineWrapper {

    private final ProcessInstanceConverter processInstanceConverter;
    private final RuntimeService runtimeService;
    private PageableProcessInstanceService pageableProcessInstanceService;
    private final TaskService taskService;
    private final TaskConverter taskConverter;
    private final PageableTaskService pageableTaskService;

    @Autowired
    public ProcessEngineWrapper(ProcessInstanceConverter processInstanceConverter,
                                RuntimeService runtimeService,
                                PageableProcessInstanceService pageableProcessInstanceService,
                                TaskService taskService,
                                TaskConverter taskConverter,
                                PageableTaskService pageableTaskService,
                                MessageProducerActivitiEventListener listener) {
        this.processInstanceConverter = processInstanceConverter;
        this.runtimeService = runtimeService;
        this.pageableProcessInstanceService = pageableProcessInstanceService;
        this.taskService = taskService;
        this.taskConverter = taskConverter;
        this.pageableTaskService = pageableTaskService;
        this.runtimeService.addEventListener(listener);
    }

    public Page<ProcessInstance> getProcessInstances(Pageable pageable) {
        return pageableProcessInstanceService.getProcessInstances(pageable);
    }

    public ProcessInstance startProcess(StartProcessInstanceCmd cmd) {
        ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder();
        builder.processDefinitionId(cmd.getProcessDefinitionId());
        builder.variables(cmd.getVariables());
        return processInstanceConverter.from(builder.start());
    }

    public void signal(SignalProcessInstancesCmd signalProcessInstancesCmd) {
        runtimeService.signalEventReceived(signalProcessInstancesCmd.getName(),
                                           signalProcessInstancesCmd.getInputVariables());
    }

    public void suspend(SuspendProcessInstanceCmd suspendProcessInstanceCmd) {
        runtimeService.suspendProcessInstanceById(suspendProcessInstanceCmd.getProcessInstanceId());
    }

    public void activate(ActivateProcessInstanceCmd activateProcessInstanceCmd) {
        runtimeService.activateProcessInstanceById(activateProcessInstanceCmd.getProcessInstanceId());
    }

    public ProcessInstance getProcessInstanceById(String processInstanceId) {
        org.activiti.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        return processInstanceConverter.from(processInstance);
    }

    public List<String> getActiveActivityIds(String executionId) {
        return runtimeService.getActiveActivityIds(executionId);
    }

    public Page<Task> getTasks(Pageable pageable) {
        return pageableTaskService.getTasks(pageable);
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
        Map<String, Object> outputVariables = null;
        if (completeTaskCmd != null) {
            outputVariables = completeTaskCmd.getOutputVariables();
        }
        taskService.complete(completeTaskCmd.getTaskId(),
                             outputVariables);
    }

    public void setTaskVariables(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariables(setTaskVariablesCmd.getTaskId(),
                                 setTaskVariablesCmd.getVariables());
    }

    public void setTaskVariablesLocal(SetTaskVariablesCmd setTaskVariablesCmd) {
        taskService.setVariablesLocal(setTaskVariablesCmd.getTaskId(),
                                      setTaskVariablesCmd.getVariables());
    }
}
