package org.activiti.cloud.services.core;

import java.util.UUID;

import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.runtime.api.model.FluentProcessInstance;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.impl.FluentProcessInstanceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessEngineWrapperTest {

    @InjectMocks
    private ProcessEngineWrapper processEngineWrapper;

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

    @Mock
    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @Before
    public void setUp() {
        initMocks(this);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("anonymous");
    }

    @Test
    public void shouldCompleteTask() {
        processEngineWrapper.completeTask(mock(CompleteTaskCmd.class));
        verify(taskService).complete(any(),
                                     any());
    }

    @Test
    public void shouldClaimTask() {
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId(any())).thenReturn(query);
        when(taskService.createTaskQuery()).thenReturn(query);
        Task task = mock(Task.class);
        when(query.singleResult()).thenReturn(task);
        when(taskConverter.from(task)).thenReturn(mock(org.activiti.cloud.services.api.model.Task.class));
        processEngineWrapper.claimTask(mock(ClaimTaskCmd.class));
        verify(taskService).claim(any(),
                                  any());
    }

    @Test
    public void shouldReleaseTask() {
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
    public void shouldSetTaskVariables() {
        processEngineWrapper.setTaskVariables(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariables(any(),
                                         any());
    }

    @Test
    public void shouldSetTaskVariablesLocal() {
        processEngineWrapper.setTaskVariablesLocal(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariablesLocal(any(),
                                              any());
    }

    private ProcessDefinition buildProcessDefinition(String processDefinitionKey) {
        ProcessDefinition def = mock(ProcessDefinition.class);
        given(def.getKey()).willReturn(processDefinitionKey);
        return def;
    }

    private FluentProcessInstance buildProcessInstance(String processInstanceId,
                                                       String processDefinitionId) {
        FluentProcessInstanceImpl processInstance = new FluentProcessInstanceImpl(null, null);
        processInstance.setId(processInstanceId);
        processInstance.setProcessDefinitionId(processDefinitionId);
        return processInstance;
    }

    /**
     * Test that delete task method on process engine wrapper
     * will trigger delete task method on process engine
     * if the task exists.
     */
    @Test
    public void testDeleteTask() {
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
        verify(taskService).deleteTask(eq("taskId"),
                                       startsWith("Cancelled by"));
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

    private ProcessInstance aProcessInstanceWithWritePermission(boolean hasWritePermission) {
        FluentProcessInstance processInstance = buildProcessInstance(UUID.randomUUID().toString(),
                                                                     UUID.randomUUID().toString());
        given(securityAwareProcessInstanceService.getAuthorizedProcessInstanceById(processInstance.getId())).willReturn(processInstance);
        ProcessDefinition def = buildProcessDefinition("my-proc");
        when(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId())).thenReturn(def);

        when(securityService.canWrite(def.getKey())).thenReturn(hasWritePermission);
        return processInstance;
    }

}
