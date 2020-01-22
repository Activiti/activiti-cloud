package org.activiti.cloud.services.modeling.service;


import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.service.api.ModelService.ProjectAccessControl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ModelService modelService;

    @Mock
    private Project project;

    @Mock
    private UserTask taskOne;

    @Mock
    private UserTask taskTwo;

    @Test
    public void should_getUsersAndGroupsBelongingToAProject_when_getProcessAccessControl() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(asList("groupOne", "groupTwo"));
        when(taskOne.getCandidateUsers()).thenReturn(asList("userOne", "userTwo"));
        when(taskTwo.getAssignee()).thenReturn("userThree");
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
                .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups())
                .hasSize(2)
                .contains("groupOne", "groupTwo");
        assertThat(projectAccessControl.getUsers())
                .hasSize(3)
                .contains("userOne", "userTwo", "userThree");
    }

    @Test
    public void should_getUsersAndGroupsBelongingToAProjectExludingExpressions_when_getProcessAccessControl() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(asList("groupOne", "${processsVariable.groupName}", "groupTwo"));
        when(taskOne.getCandidateUsers()).thenReturn(asList("${username_Var}", "userOne"));
        when(taskTwo.getAssignee()).thenReturn("${processsVariable.username}");
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
                .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups())
                .hasSize(2)
                .contains("groupOne", "groupTwo");
        assertThat(projectAccessControl.getUsers())
                .hasSize(1)
                .contains("userOne");
    }

    @Test
    public void should_returnEmptyLists_when_thereAreNotAssigneeAndCandidateUsersAndGroups() {
        List<UserTask> userTasks = asList(taskOne, taskTwo);

        when(taskOne.getCandidateGroups()).thenReturn(null);
        when(taskOne.getCandidateUsers()).thenReturn(null);
        when(taskTwo.getAssignee()).thenReturn(null);
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
                .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).isEmpty();
        assertThat(projectAccessControl.getUsers()).isEmpty();
    }


    @Test
    public void should_returnEmptyLists_when_thereAreNotUserTasks() {
        List<UserTask> userTasks = new LinkedList<>();
        when(modelService.getTasksBy(eq(project), any(ProcessModelType.class), eq(UserTask.class)))
                .thenReturn(userTasks);

        ProjectAccessControl projectAccessControl = projectService.getProjectAccessControl(project);

        assertThat(projectAccessControl.getGroups()).isEmpty();
        assertThat(projectAccessControl.getUsers()).isEmpty();
    }
}