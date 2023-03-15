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

import java.util.Date;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskEntityAssignedEventHandlerTest {

    @InjectMocks
    private TaskAssignedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldUpdateTaskStatusToAssigned() {
        //given
        CloudTaskAssignedEvent event = buildTaskAssignedEvent();

        String taskId = event.getEntity().getId();
        TaskEntity taskEntity = aTask().build();

        given(entityManager.find(TaskEntity.class, taskId)).willReturn(taskEntity);

        //when
        handler.handle(event);

        //then
        verify(entityManager).persist(taskEntity);
        verify(taskEntity).setStatus(Task.TaskStatus.ASSIGNED);
        verify(taskEntity).setAssignee(event.getEntity().getAssignee());
        verify(taskEntity).setLastModified(any(Date.class));
    }

    private CloudTaskAssignedEvent buildTaskAssignedEvent() {
        TaskImpl task = new TaskImpl(UUID.randomUUID().toString(), "task", Task.TaskStatus.ASSIGNED);
        task.setAssignee("user");
        return new CloudTaskAssignedEventImpl(task);
    }

    @Test
    public void handleShouldThrowExceptionWhenNoTaskIsFoundForTheGivenId() {
        //given
        CloudTaskAssignedEvent event = buildTaskAssignedEvent();

        String taskId = event.getEntity().getId();
        given(entityManager.find(TaskEntity.class, taskId)).willReturn(null);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessageContaining("Unable to find task with id: " + taskId);
    }

    @Test
    public void getHandledEventShouldReturnTaskAssignedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name());
    }
}
