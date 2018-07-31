/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.core.pageable;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.runtime.api.TaskRuntime;
import org.activiti.runtime.api.model.payloads.ClaimTaskPayload;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
import org.activiti.runtime.api.model.payloads.DeleteTaskPayload;
import org.activiti.runtime.api.model.payloads.ReleaseTaskPayload;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecurityAwareTaskServiceTest {

    @InjectMocks
    private SecurityAwareTaskService taskService;

    @Mock
    private TaskRuntime taskRuntime;

    @Mock
    private SpringSecurityAuthenticationWrapper authenticationWrapper;

    @Before
    public void setUp() {
        initMocks(this);
        given(authenticationWrapper.getAuthenticatedUserId()).willReturn("joan");
    }

    /**
     * Test that delete task method on process engine wrapper
     * will trigger delete task method on process engine
     * if the task exists.
     */
    @Test
    public void deleteTaskShouldCallDeleteOnFluentTask() {
        //GIVEN
        DeleteTaskPayload deleteTaskPayload = mock(DeleteTaskPayload.class);

        //WHEN
        taskService.deleteTask(deleteTaskPayload);

        //THEN
        verify(taskRuntime).delete(deleteTaskPayload);
    }

    @Test
    public void claimShouldCallClaimOnFluentTask() {
        //given
        ClaimTaskPayload claimTaskPayload = mock(ClaimTaskPayload.class);

        //when
        taskService.claimTask(claimTaskPayload);

        //then
        verify(taskRuntime).claim(claimTaskPayload);
    }

    @Test
    public void releaseTaskShouldClearAssignee() {
        //given
        ReleaseTaskPayload releaseTaskPayload = mock(ReleaseTaskPayload.class);

        //when
        taskService.releaseTask(releaseTaskPayload);

        //then
        verify(taskRuntime).release(releaseTaskPayload);
    }

    @Test
    public void completeTaskShouldCallCompleteOnTaskRuntime() {
        //given
        CompleteTaskPayload payload = mock(CompleteTaskPayload.class);

        //when
        taskService.completeTask(payload);

        //then
        verify(taskRuntime).complete(payload);
    }

    @Test
    public void setTaskVariablesShouldSetVariablesOnFluentTask() {
        //given
        SetTaskVariablesPayload payload = mock(SetTaskVariablesPayload.class);

        //when
        taskService.setTaskVariables(payload);

        //then
        verify(taskRuntime).setVariables(payload);
    }

}