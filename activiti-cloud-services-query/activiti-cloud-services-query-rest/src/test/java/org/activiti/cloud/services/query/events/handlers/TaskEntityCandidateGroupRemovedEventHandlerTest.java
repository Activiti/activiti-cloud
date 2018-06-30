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
import org.activiti.cloud.services.query.model.TaskCandidateGroup;
import org.activiti.runtime.api.event.TaskCandidateGroupEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.runtime.api.model.impl.TaskCandidateGroupImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskEntityCandidateGroupRemovedEventHandlerTest {

    @InjectMocks
    private TaskCandidateGroupRemovedEventHandler handler;

    @Mock
    private TaskCandidateGroupRepository taskCandidateRepository;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldDeleteTaskGroupCandidate() {
        //given
        CloudTaskCandidateGroupRemovedEventImpl event = buildTaskCandidateEvent();

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskCandidateGroup> captor = ArgumentCaptor.forClass(TaskCandidateGroup.class);
        verify(taskCandidateRepository).delete(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    private CloudTaskCandidateGroupRemovedEventImpl buildTaskCandidateEvent() {
        TaskCandidateGroupImpl taskCandidateGroup = new TaskCandidateGroupImpl(UUID.randomUUID().toString(),
                                                                               UUID.randomUUID().toString());
        return new CloudTaskCandidateGroupRemovedEventImpl(taskCandidateGroup);
    }

    @Test
    public void getHandledEventShouldReturnTaskCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name());
    }
}