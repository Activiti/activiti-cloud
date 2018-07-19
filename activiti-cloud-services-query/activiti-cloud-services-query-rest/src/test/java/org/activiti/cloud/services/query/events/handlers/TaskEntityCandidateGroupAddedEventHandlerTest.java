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

import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.runtime.api.event.CloudTaskCandidateGroupAddedEvent;
import org.activiti.runtime.api.event.TaskCandidateGroupEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.runtime.api.model.impl.TaskCandidateGroupImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskEntityCandidateGroupAddedEventHandlerTest {

    @InjectMocks
    private TaskCandidateGroupAddedEventHandler handler;

    @Mock
    private TaskCandidateGroupRepository taskCandidateRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldStoreNewTaskCandidateGroup() {
        //given
        CloudTaskCandidateGroupAddedEvent event = buildTaskCandidateGroupAddedEvent();
        //when
        handler.handle(event);

        //then
        ArgumentCaptor<org.activiti.cloud.services.query.model.TaskCandidateGroup> captor = ArgumentCaptor.forClass(org.activiti.cloud.services.query.model.TaskCandidateGroup.class);
        verify(taskCandidateRepository).save(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    @Test
    public void handleShouldThrowExceptionWhenUnableToSave() {
        //given
        CloudTaskCandidateGroupAddedEvent event = buildTaskCandidateGroupAddedEvent();
        Exception cause = new RuntimeException("Something went wrong");
        given(taskCandidateRepository.save(any())).willThrow(cause);

        //when
        Throwable throwable = catchThrowable(() -> handler.handle(event));

        //then
        assertThat(throwable)
                .isInstanceOf(QueryException.class)
                .hasCause(cause)
                .hasMessageContaining("Error handling TaskCandidateGroupAddedEvent[");
    }

    private CloudTaskCandidateGroupAddedEvent buildTaskCandidateGroupAddedEvent() {
        return new CloudTaskCandidateGroupAddedEventImpl(new TaskCandidateGroupImpl(UUID.randomUUID().toString(),
                                                                                    UUID.randomUUID().toString()));
    }

    @Test
    public void getHandledEventShouldReturnTaskCandidateGroupAddedEvent() {
        //when
        String event = handler.getHandledEvent();

        //then
        assertThat(event).isEqualTo(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED.name());
    }
}