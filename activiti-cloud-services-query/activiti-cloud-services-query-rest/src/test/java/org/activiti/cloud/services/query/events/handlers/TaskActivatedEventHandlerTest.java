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

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.events.TaskActivatedEvent;
import org.activiti.cloud.services.query.events.TaskSuspendedEvent;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.engine.ActivitiException;
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

public class TaskActivatedEventHandlerTest {

    @InjectMocks
    private TaskActivatedEventHandler handler;

    @Mock
    private TaskRepository taskRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateTaskStatusToCreated() throws Exception {
        //given
        String taskId = "30";
        Task task = aTask()
                .withId(taskId)
                .build();

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        //when
        handler.handle(new TaskActivatedEvent(System.currentTimeMillis(),
                                              "taskActivated",
                                              "10",
                                              "100",
                                              "200",
                                              "runtime-bundle-a",
                                              task));

        //then
        verify(taskRepository).save(task);
        verify(task).setStatus("CREATED");
        verify(task).setLastModified(any(Date.class));
    }

    @Test
    public void handleShouldUpdateTaskStatusToAssigned() throws Exception {
        //given
        String taskId = "30";
        Task task = aTask()
                .withAssignee("user")
                .withId(taskId)
                .build();

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        //when
        handler.handle(new TaskActivatedEvent(System.currentTimeMillis(),
                                              "taskActivated",
                                              "10",
                                              "100",
                                              "200",
                                              "runtime-bundle-a",
                                              task));

        //then
        verify(taskRepository).save(task);
        verify(task).setStatus("ASSIGNED");
        verify(task).setLastModified(any(Date.class));
    }

    @Test
    public void handleShouldThrowExceptionWhenNoTaskIsFoundForTheGivenId() throws Exception {
        //given
        String taskId = "30";
        Task task = aTask().withId(taskId).build();

        given(taskRepository.findById(taskId)).willReturn(Optional.empty());

        //then
        expectedException.expect(ActivitiException.class);
        expectedException.expectMessage("Unable to find task with id: " + taskId);

        //when
        handler.handle(new TaskActivatedEvent(System.currentTimeMillis(),
                                              "taskActivated",
                                              "10",
                                              "100",
                                              "200",
                                              "runtime-bundle-a",
                                              task));
    }

    @Test
    public void getHandledEventClassShouldReturnTaskAssignedEventClass() throws Exception {
        //when
        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();

        //then
        assertThat(handledEventClass).isEqualTo(TaskActivatedEvent.class);
    }
}