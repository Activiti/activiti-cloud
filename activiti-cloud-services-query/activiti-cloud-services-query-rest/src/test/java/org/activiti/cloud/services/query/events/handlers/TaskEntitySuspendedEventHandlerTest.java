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

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskSuspendedEventImpl;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.impl.TaskImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.query.events.handlers.TaskBuilder.aTask;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskEntitySuspendedEventHandlerTest {

    @InjectMocks
    private TaskSuspendedEventHandler handler;

    @Mock
    private TaskRepository taskRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateTaskStatusToSuspended() {
        //given
        CloudTaskSuspendedEventImpl event = buildTaskSuspendedEvent();
        String taskId = event.getEntity().getId();
        TaskEntity taskEntity = aTask()
                .withId(taskId)
                .build();

        given(taskRepository.findById(taskId)).willReturn(Optional.of(taskEntity));

        //when
        handler.handle(event);

        //then
        verify(taskRepository).save(taskEntity);
        verify(taskEntity).setStatus(Task.TaskStatus.SUSPENDED);
        verify(taskEntity).setLastModified(any(Date.class));
    }

    private CloudTaskSuspendedEventImpl buildTaskSuspendedEvent() {
        return new CloudTaskSuspendedEventImpl(new TaskImpl(UUID.randomUUID().toString(),
                                                            "task",
                                                            Task.TaskStatus.SUSPENDED));
    }

    @Test
    public void handleShouldThrowExceptionWhenNoTaskIsFoundForTheGivenId() {
        //given
        CloudTaskSuspendedEventImpl event = buildTaskSuspendedEvent();
        String taskId = event.getEntity().getId();

        given(taskRepository.findById(taskId)).willReturn(Optional.empty());

        //then
        expectedException.expect(QueryException.class);
        expectedException.expectMessage("Unable to find task with id: " + taskId);

        //when
        handler.handle(event);
    }

    @Test
    public void getHandledEventShouldReturnTaskSuspendedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name());
    }
}