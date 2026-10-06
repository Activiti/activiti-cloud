/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskUpdatedEventHandlerTest {

    @InjectMocks
    private TaskUpdatedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    void handleShouldCopyAllowSelfServiceOntoExistingTask() {
        //given
        TaskImpl eventTask = new TaskImpl("id", "name", Task.TaskStatus.ASSIGNED);
        eventTask.setAllowSelfService(true);
        CloudTaskUpdatedEventImpl event = new CloudTaskUpdatedEventImpl(eventTask);

        TaskEntity existingTask = new TaskEntity();
        existingTask.setId("id");
        when(entityManager.find(TaskEntity.class, "id")).thenReturn(existingTask);

        //when
        handler.handle(event);

        //then
        verify(entityManager).persist(existingTask);
        assertThat(existingTask.isAllowSelfService()).isTrue();
    }

    @Test
    void getHandledEventShouldReturnTaskUpdatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name());
    }
}
