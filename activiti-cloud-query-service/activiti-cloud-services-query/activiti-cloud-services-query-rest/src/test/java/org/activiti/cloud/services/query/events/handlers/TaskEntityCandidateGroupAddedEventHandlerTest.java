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
package org.activiti.cloud.services.query.events.handlers;

import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupAddedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskEntityCandidateGroupAddedEventHandlerTest {

    @InjectMocks
    private TaskCandidateGroupAddedEventHandler handler;

    @Mock
    private TaskCandidateGroupRepository taskCandidateRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldStoreNewTaskCandidateGroup() {
        //given
        CloudTaskCandidateGroupAddedEvent event = buildTaskCandidateGroupAddedEvent();
        //when
        handler.handle(event);

        //then
        ArgumentCaptor<TaskCandidateGroupEntity> captor = ArgumentCaptor.forClass(TaskCandidateGroupEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    @Test
    public void handleShouldNotStoreTaskCandidateGroupIfExists() {
        //given
        CloudTaskCandidateGroupAddedEvent event = buildTaskCandidateGroupAddedEvent();
        TaskCandidateGroup entity = event.getEntity();

        //when
        handler.handle(event);
        when(entityManager.find(TaskCandidateGroupEntity.class, new TaskCandidateGroupId(entity.getTaskId(), entity.getGroupId())))
            .thenReturn(new TaskCandidateGroupEntity());
        handler.handle(event);

        //then
        ArgumentCaptor<TaskCandidateGroupEntity> captor = ArgumentCaptor.forClass(TaskCandidateGroupEntity.class);
        verify(entityManager, times(1)).persist(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(event.getEntity().getTaskId());
        assertThat(captor.getValue().getGroupId()).isEqualTo(event.getEntity().getGroupId());
    }

    @Test
    public void handleShouldThrowExceptionWhenUnableToSave() {
        //given
        CloudTaskCandidateGroupAddedEvent event = buildTaskCandidateGroupAddedEvent();
        Exception cause = new RuntimeException("Something went wrong");
        doThrow(cause).when(entityManager).persist(any(TaskCandidateGroupEntity.class));

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
