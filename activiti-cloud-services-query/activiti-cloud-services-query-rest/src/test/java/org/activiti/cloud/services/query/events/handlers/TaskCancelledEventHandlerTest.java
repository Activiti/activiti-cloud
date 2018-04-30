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
import org.activiti.cloud.services.query.events.TaskCancelledEvent;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.engine.ActivitiException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.query.events.mock.MockEvents.taskCancelledEvent;
import static org.activiti.cloud.services.query.events.handlers.TaskBuilder.aTask;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link TaskCancelledEventHandler}
 */
public class TaskCancelledEventHandlerTest {

    private final static String TASK_ID = "testTaskId";

    @InjectMocks
    private TaskCancelledEventHandler handler;

    @Mock
    private TaskRepository taskRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateTaskStatusToCancelled() {
        //given
        Task eventTask = aTask().withId(TASK_ID).build();
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(eventTask));

        //when
        handler.handle(taskCancelledEvent(TASK_ID).get());

        //then
        verify(taskRepository).save(eventTask);
        verify(eventTask).setStatus("CANCELLED");
        verify(eventTask).setLastModified(any(Date.class));
    }

    @Test
    public void testThrowExceptionWhenTaskNotFound() {
        //given
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.empty());

        //then
        expectedException.expect(ActivitiException.class);
        expectedException.expectMessage("Unable to find task with id: " + TASK_ID);

        //when
        handler.handle(taskCancelledEvent(TASK_ID).get());
    }

    @Test
    public void getHandledEventClass() {
        //when
        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();

        //then
        assertThat(handledEventClass).isEqualTo(TaskCancelledEvent.class);
    }
}