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

import static org.activiti.cloud.services.query.events.handlers.TaskBuilder.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Date;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskUpdatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TaskEntityUpdatedEventHandlerTest {

    @InjectMocks
    private TaskUpdatedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateTask() {
        //given
        CloudTaskUpdatedEvent event = buildTaskUpdateEvent();
        String taskId = event.getEntity().getId();
        TaskEntity eventTaskEntity = aTask()
            .withId(taskId)
            .withName("name")
            .withDescription("description")
            .withPriority(10)
            .withFormKey("formKey")
            .build();

        given(entityManager.find(TaskEntity.class, taskId))
            .willReturn(eventTaskEntity);

        //when
        handler.handle(event);

        //then
        verify(entityManager).persist(eventTaskEntity);
        verify(eventTaskEntity).setName(event.getEntity().getName());
        verify(eventTaskEntity)
            .setDescription(event.getEntity().getDescription());
        verify(eventTaskEntity).setPriority(event.getEntity().getPriority());
        verify(eventTaskEntity).setDueDate(event.getEntity().getDueDate());
        verify(eventTaskEntity).setFormKey(event.getEntity().getFormKey());
        verify(eventTaskEntity)
            .setParentTaskId(event.getEntity().getParentTaskId());
        verify(eventTaskEntity).setLastModified(any(Date.class));
        verify(eventTaskEntity).setStatus(event.getEntity().getStatus());

        verifyNoMoreInteractions(eventTaskEntity);
    }

    private CloudTaskUpdatedEventImpl buildTaskUpdateEvent() {
        final TaskImpl task = new TaskImpl(
            UUID.randomUUID().toString(),
            "my task",
            Task.TaskStatus.ASSIGNED
        );
        task.setAssignee("user");
        task.setDescription("task description");
        task.setPriority(75);
        task.setDueDate(new Date());
        return new CloudTaskUpdatedEventImpl(task);
    }

    @Test
    public void handleShouldThrowAnExceptionWhenNoTaskIsFoundForTheGivenId() {
        //given
        CloudTaskUpdatedEventImpl event = buildTaskUpdateEvent();
        String taskId = event.getEntity().getId();
        given(entityManager.find(TaskEntity.class, taskId)).willReturn(null);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessageContaining("Unable to find task with id: " + taskId);
    }

    @Test
    public void getHandledEventShouldReturnTaskUpdatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent)
            .isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name());
    }
}
