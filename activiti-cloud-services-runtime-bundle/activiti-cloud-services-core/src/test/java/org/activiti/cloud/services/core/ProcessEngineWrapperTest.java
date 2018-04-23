package org.activiti.cloud.services.core;

import java.util.UUID;

import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.converter.ProcessInstanceConverter;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.RuntimeServiceImpl;
import org.activiti.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessEngineWrapperTest {

    @InjectMocks
    private ProcessEngineWrapper processEngineWrapper;

    @Mock
    private ProcessInstanceConverter processInstanceConverter;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private TaskConverter taskConverter;
    @Mock
    private SecurityPoliciesApplicationService securityService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private AuthenticationWrapper authenticationWrapper;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("anonymous");
    }

    @Test
    public void shouldNotStartWithoutPermission(){
        when(securityService.canWrite(any())).thenReturn(false);
        when(repositoryService.getProcessDefinition(any())).thenReturn(mock(ProcessDefinition.class));
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(() -> processEngineWrapper.startProcess(mock(StartProcessInstanceCmd.class)));
    }
    @Test
    public void shouldStartProcessWithPermission(){
        when(securityService.canWrite(any())).thenReturn(true);
        ProcessInstanceBuilder builder = mock(ProcessInstanceBuilder.class);
        when(runtimeService.createProcessInstanceBuilder()).thenReturn(builder);
        org.activiti.engine.runtime.ProcessInstance engineInst = mock(org.activiti.engine.runtime.ProcessInstance.class);
        when(builder.start()).thenReturn(engineInst);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(processInstanceConverter.from(engineInst)).thenReturn(instance);
        when(repositoryService.getProcessDefinition(any())).thenReturn(mock(ProcessDefinition.class));
        assertThat(processEngineWrapper.startProcess(mock(StartProcessInstanceCmd.class))).isEqualTo(instance);
    }

    @Test
    public void shouldNotStartProcessWithNoProcessDefinitionKey(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey(any())).thenReturn(query);
        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd("processDefKey", null, null, null);
        assertThatExceptionOfType(ActivitiObjectNotFoundException.class).isThrownBy(() -> processEngineWrapper.startProcess(cmd));
    }

    @Test
    public void shouldStartProcessWithProcessDefinitionKey(){
        when(securityService.canWrite(any())).thenReturn(true);
        RuntimeServiceImpl runtimeServiceImpl = mock(RuntimeServiceImpl.class);
        ProcessInstanceBuilderImpl builder = new ProcessInstanceBuilderImpl(runtimeServiceImpl);
        when(runtimeService.createProcessInstanceBuilder()).thenReturn(builder);
        org.activiti.engine.runtime.ProcessInstance engineInst = mock(org.activiti.engine.runtime.ProcessInstance.class);
        when(builder.start()).thenReturn(engineInst);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(processInstanceConverter.from(engineInst)).thenReturn(instance);
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey(any())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        StartProcessInstanceCmd cmd = new StartProcessInstanceCmd("processDefKey", null, null, null);
        processEngineWrapper.startProcess(cmd);
        assertThat(builder.getProcessDefinitionKey()).isEqualTo("processDefKey");
    }

    @Test
    public void shouldSignal(){
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(securityService.restrictProcessDefQuery(query, SecurityPolicy.WRITE)).thenReturn(query);
        when(query.count()).thenReturn(1L);
        processEngineWrapper.signal(mock(SignalProcessInstancesCmd.class));
        verify(runtimeService).signalEventReceived(any(),anyMap());
    }
    @Test
    public void shouldNotSuspendWithoutPermission(){
        ProcessDefinition def = mock(ProcessDefinition.class);
        org.activiti.engine.runtime.ProcessInstance inst = mock(org.activiti.engine.runtime.ProcessInstance.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(any())).thenReturn(query);
        when(query.singleResult()).thenReturn(inst);
        when(processInstanceConverter.from(inst)).thenReturn(mock(ProcessInstance.class));
        when(repositoryService.getProcessDefinition(any())).thenReturn(def);
        when(securityService.canWrite(any())).thenReturn(false);
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(() -> processEngineWrapper.suspend(mock(SuspendProcessInstanceCmd.class)));
    }
    @Test
    public void shouldNotActivateWithoutPermission(){
        ProcessDefinition def = mock(ProcessDefinition.class);
        org.activiti.engine.runtime.ProcessInstance inst = mock(org.activiti.engine.runtime.ProcessInstance.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(any())).thenReturn(query);
        when(query.singleResult()).thenReturn(inst);
        when(processInstanceConverter.from(inst)).thenReturn(mock(ProcessInstance.class));
        when(repositoryService.getProcessDefinition(any())).thenReturn(def);
        when(securityService.canWrite(any())).thenReturn(false);
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(() -> processEngineWrapper.activate(mock(ActivateProcessInstanceCmd.class)));
    }
    @Test
    public void shouldCompleteTask(){
        processEngineWrapper.completeTask(mock(CompleteTaskCmd.class));
        verify(taskService).complete(any(),any());
    }
    @Test
    public void shouldClaimTask(){
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId(any())).thenReturn(query);
        when(taskService.createTaskQuery()).thenReturn(query);
        Task task = mock(Task.class);
        when(query.singleResult()).thenReturn(task);
        when(taskConverter.from(task)).thenReturn(mock(org.activiti.cloud.services.api.model.Task.class));
        processEngineWrapper.claimTask(mock(ClaimTaskCmd.class));
        verify(taskService).claim(any(),any());
    }
    @Test
    public void shouldReleaseTask(){
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId(any())).thenReturn(query);
        when(taskService.createTaskQuery()).thenReturn(query);
        Task task = mock(Task.class);
        when(query.singleResult()).thenReturn(task);
        when(taskConverter.from(task)).thenReturn(mock(org.activiti.cloud.services.api.model.Task.class));
        processEngineWrapper.releaseTask(mock(ReleaseTaskCmd.class));
        verify(taskService).unclaim(any());
    }
    @Test
    public void shouldSetTaskVariables(){
        processEngineWrapper.setTaskVariables(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariables(any(),any());
    }
    @Test
    public void shouldSetTaskVariablesLocal(){
        processEngineWrapper.setTaskVariablesLocal(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariablesLocal(any(),any());
    }

    @Test
    public void testDeleteNotExistingProcessInstance() {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(any())).thenReturn(query);
        assertThatExceptionOfType(ActivitiException.class).isThrownBy(
                () -> processEngineWrapper.deleteProcessInstance(UUID.randomUUID().toString())
        ).withMessageStartingWith("Unable to find process instance for the given id:");
    }

    @Test
    public void testDeleteProcessInstanceForNotExistingProcessDefinition() {
        org.activiti.engine.runtime.ProcessInstance processInstance =
                mock(org.activiti.engine.runtime.ProcessInstance.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(any())).thenReturn(query);
        when(query.singleResult()).thenReturn(processInstance);
        when(processInstanceConverter.from(processInstance)).thenReturn(mock(ProcessInstance.class));

        assertThatExceptionOfType(ActivitiException.class).isThrownBy(
                () -> processEngineWrapper.deleteProcessInstance(UUID.randomUUID().toString())
        ).withMessageStartingWith("Unable to find process definition for the given id:");
    }

    @Test
    public void shouldNotDeleteProcessInstanceWithoutPermission() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        org.activiti.engine.runtime.ProcessInstance processInstance =
                mock(org.activiti.engine.runtime.ProcessInstance.class);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(any())).thenReturn(query);
        when(query.singleResult()).thenReturn(processInstance);
        when(processInstanceConverter.from(processInstance)).thenReturn(mock(ProcessInstance.class));
        when(repositoryService.getProcessDefinition(any())).thenReturn(def);

        when(securityService.canWrite(any())).thenReturn(false);
        assertThatExceptionOfType(ActivitiForbiddenException.class).isThrownBy(
                () -> processEngineWrapper.deleteProcessInstance(processInstance.getProcessInstanceId())
        ).withMessageStartingWith("Operation not permitted");
    }

    /**
     * Test that delete task method on process engine wrapper
     * will trigger delete task method on process engine
     * if the task exists.
     */
    @Test
    public void testDeleteTask(){
        //GIVEN
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId(eq("taskId"))).thenReturn(query);
        when(taskService.createTaskQuery()).thenReturn(query);
        Task task = mock(Task.class);
        when(query.singleResult()).thenReturn(task);

        org.activiti.cloud.services.api.model.Task modelTask = mock(org.activiti.cloud.services.api.model.Task.class);
        modelTask.setId("taskId");

        when(taskConverter.from(task)).thenReturn(modelTask);

        //WHEN
        processEngineWrapper.deleteTask("taskId");

        //THEN
        verify(taskService).deleteTask(eq("taskId"), startsWith("Cancelled by"));
    }

    /**
     * Test that delete task method on process engine wrapper
     * will throw ActivitiObjectNotFoundException
     * if the task doesn't exist.
     */
    @Test
    public void testDeleteTaskForNotExistingTask() {
        //GIVEN
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId(eq("not-existent-task"))).thenReturn(query);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        //THEN
        assertThatExceptionOfType(ActivitiObjectNotFoundException.class).isThrownBy(
                //WHEN
                () -> processEngineWrapper.deleteTask("not-existent-task")
        ).withMessage("Unable to find task for the given id: not-existent-task");
    }
}
