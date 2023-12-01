/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import static org.activiti.cloud.services.query.events.handlers.TaskBuilder.aTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.api.task.model.QueryCloudTask.TaskPermissions;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class TaskPermissionsHelperTest {

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private TaskControllerHelper taskControllerHelper;

    @Autowired
    private TaskPermissionsHelper taskPermissionsHelper;

    @Test
    public void should_not_addAnyPermission_when_canNotViewTask() {
        TaskEntity taskEntity = aTask().withId("task1").withCandidateUsers(List.of("testuser")).build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(false);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(0)).setPermissions(any());
    }

    @Test
    public void should_addClaimPermission_when_isCandidateUser() {
        TaskEntity taskEntity = aTask().withId("task1").withCandidateUsers(List.of("testuser")).build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1)).setPermissions(List.of(TaskPermissions.VIEW, TaskPermissions.CLAIM));
    }

    @Test
    public void should_not_addClaimPermission_when_taskIsAssignedToAnotherUser() {
        TaskEntity taskEntity = aTask().withId("task1").withAssignee("user").build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1)).setPermissions(List.of(TaskPermissions.VIEW));
    }

    @Test
    public void should_addClaimPermission_when_isCandidateGroup() {
        TaskEntity taskEntity = aTask().withId("task1").withCandidateGroups(List.of("testgroup")).build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(securityManager.getAuthenticatedUserGroups()).willReturn(List.of("testgroup"));
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1)).setPermissions(List.of(TaskPermissions.VIEW, TaskPermissions.CLAIM));
    }

    @Test
    public void should_not_addClaimPermission_when_isOwner() {
        TaskEntity taskEntity = aTask().withId("task1").withOwner("testuser").build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1)).setPermissions(List.of(TaskPermissions.VIEW, TaskPermissions.UPDATE));
    }

    @Test
    public void should_addUpdatePermission_when_UserIsAssignee() {
        TaskEntity taskEntity = aTask().withId("task1").withAssignee("testuser").build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1)).setPermissions(List.of(TaskPermissions.VIEW, TaskPermissions.UPDATE));
    }

    @Test
    public void should_addReleasePermission_when_UserIsAssignee() {
        TaskEntity taskEntity = aTask()
            .withId("task1")
            .withAssignee("testuser")
            .withCandidateUsers(List.of("user1"))
            .build();
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");
        given(taskControllerHelper.canUserViewTask(any())).willReturn(true);

        taskPermissionsHelper.setCurrentUserTaskPermissions(taskEntity);

        verify(taskEntity, times(1))
            .setPermissions(List.of(TaskPermissions.VIEW, TaskPermissions.RELEASE, TaskPermissions.UPDATE));
    }
}
