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

package org.activiti.cloud.services.query.events.handlers;

import java.util.UUID;

import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.model.TaskCandidateUser;
import org.activiti.runtime.api.event.TaskCandidateUserEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.runtime.api.model.impl.TaskCandidateUserImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskEntityCandidateUserRemovedEventHandlerTest {

    @InjectMocks
    private TaskCandidateUserRemovedEventHandler handler;

    @Mock
    private TaskCandidateUserRepository taskCandidateRepository;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldStoreNewTaskInstance() {
        //given
        CloudTaskCandidateUserRemovedEventImpl event = new CloudTaskCandidateUserRemovedEventImpl(
                new TaskCandidateUserImpl(UUID.randomUUID().toString(),
                                          UUID.randomUUID().toString())
        );

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskCandidateUser> captor = ArgumentCaptor.forClass(TaskCandidateUser.class);
        verify(taskCandidateRepository).delete(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getUserId()).isEqualTo(event.getEntity().getUserId());
    }

    @Test
    public void getHandledEventShouldReturnTaskCandidateUserCreated() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name());
    }
}