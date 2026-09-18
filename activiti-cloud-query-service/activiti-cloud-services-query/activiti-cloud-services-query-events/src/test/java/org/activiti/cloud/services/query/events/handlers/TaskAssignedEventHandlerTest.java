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
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskAssignedEventHandlerTest {

    private static final String TASK_ID = "task-id";

    @InjectMocks
    private TaskAssignedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    void handleShouldSetStatusAssignedWhenAssigneeIsPresent() {
        TaskEntity queryTaskEntity = givenExistingTask(Task.TaskStatus.CREATED, null);

        handler.handle(taskAssignedEvent("testuser"));

        assertThat(queryTaskEntity.getAssignee()).isEqualTo("testuser");
        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
    }

    @Test
    void handleShouldSetStatusCreatedWhenAssigneeIsNull() {
        TaskEntity queryTaskEntity = givenExistingTask(Task.TaskStatus.ASSIGNED, "testuser");

        handler.handle(taskAssignedEvent(null));

        assertThat(queryTaskEntity.getAssignee()).isNull();
        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
    }

    @Test
    void handleShouldSetStatusCreatedWhenAssigneeIsEmpty() {
        TaskEntity queryTaskEntity = givenExistingTask(Task.TaskStatus.ASSIGNED, "testuser");

        handler.handle(taskAssignedEvent(""));

        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
    }

    @Test
    void handleShouldRestoreCreatedStatusAcrossClaimReleaseClaimCycle() {
        TaskEntity queryTaskEntity = givenExistingTask(Task.TaskStatus.CREATED, null);

        handler.handle(taskAssignedEvent("testuser"));
        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);

        handler.handle(taskAssignedEvent(null));
        assertThat(queryTaskEntity.getAssignee()).isNull();
        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.CREATED);

        handler.handle(taskAssignedEvent("testuser"));
        assertThat(queryTaskEntity.getAssignee()).isEqualTo("testuser");
        assertThat(queryTaskEntity.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
    }

    @Test
    void getHandledEventShouldReturnTaskAssignedEvent() {
        assertThat(handler.getHandledEvent()).isEqualTo(TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name());
    }

    private TaskEntity givenExistingTask(Task.TaskStatus status, String assignee) {
        TaskEntity queryTaskEntity = new TaskEntity();
        queryTaskEntity.setId(TASK_ID);
        queryTaskEntity.setStatus(status);
        queryTaskEntity.setAssignee(assignee);
        when(entityManager.find(TaskEntity.class, TASK_ID)).thenReturn(queryTaskEntity);
        return queryTaskEntity;
    }

    private static CloudTaskAssignedEventImpl taskAssignedEvent(String assignee) {
        boolean assigned = assignee != null && !assignee.isEmpty();
        TaskImpl task = new TaskImpl(TASK_ID, "task", assigned ? Task.TaskStatus.ASSIGNED : Task.TaskStatus.CREATED);
        task.setAssignee(assignee);
        return new CloudTaskAssignedEventImpl("event-id", System.currentTimeMillis(), task);
    }
}
