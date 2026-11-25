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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.services.query.model.TaskAssignmentEventReceiptEntity;
import org.activiti.cloud.services.query.model.TaskAssignmentHistoryEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Test
    void handleShouldPersistAssignmentMetadataBeforeChangingTheAssignee() {
        givenExistingTask(Task.TaskStatus.ASSIGNED, "previous");
        CloudTaskAssignedEventImpl event = taskAssignedEvent("new");
        event.setActor("admin");
        event.setReason("Workload");
        event.setAssignmentAction("CLAIMED");

        handler.handle(event);

        ArgumentCaptor<TaskAssignmentHistoryEntity> captor = ArgumentCaptor.forClass(TaskAssignmentHistoryEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue())
            .returns(event.getId(), TaskAssignmentHistoryEntity::getId)
            .returns(TASK_ID, TaskAssignmentHistoryEntity::getTaskId)
            .returns("admin", TaskAssignmentHistoryEntity::getActor)
            .returns("previous", TaskAssignmentHistoryEntity::getPreviousAssignee)
            .returns("new", TaskAssignmentHistoryEntity::getNewAssignee)
            .returns("CLAIMED", TaskAssignmentHistoryEntity::getAction)
            .returns("Workload", TaskAssignmentHistoryEntity::getReason)
            .returns(event.getTimestamp(), TaskAssignmentHistoryEntity::getTimestamp);
    }

    @Test
    void handleShouldNotPersistUnchangedAssignments() {
        givenExistingTask(Task.TaskStatus.ASSIGNED, "same");

        handler.handle(taskAssignedEvent("same"));

        verify(entityManager, never()).persist(any(TaskAssignmentHistoryEntity.class));
    }

    @Test
    void handleShouldIgnoreAnAlreadyRecordedEventWithoutRevertingTheTask() {
        CloudTaskAssignedEventImpl event = taskAssignedEvent("old");
        when(entityManager.find(TaskAssignmentEventReceiptEntity.class, event.getId())).thenReturn(
            new TaskAssignmentEventReceiptEntity(event.getId(), TASK_ID)
        );

        handler.handle(event);

        verify(entityManager, never()).find(TaskEntity.class, TASK_ID);
        verify(entityManager, never()).persist(any());
    }

    @Test
    void handleShouldInferReleaseForEventsWithoutMetadata() {
        givenExistingTask(Task.TaskStatus.ASSIGNED, "previous");

        handler.handle(taskAssignedEvent(null));

        ArgumentCaptor<TaskAssignmentHistoryEntity> captor = ArgumentCaptor.forClass(TaskAssignmentHistoryEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue())
            .returns("RELEASED", TaskAssignmentHistoryEntity::getAction)
            .returns(null, TaskAssignmentHistoryEntity::getNewAssignee)
            .returns(null, TaskAssignmentHistoryEntity::getReason);
    }

    private TaskEntity givenExistingTask(Task.TaskStatus status, String assignee) {
        TaskEntity queryTaskEntity = new TaskEntity();
        queryTaskEntity.setId(TASK_ID);
        queryTaskEntity.setStatus(status);
        queryTaskEntity.setAssignee(assignee);
        when(entityManager.find(eq(TaskAssignmentEventReceiptEntity.class), anyString())).thenReturn(null);
        when(entityManager.find(TaskEntity.class, TASK_ID)).thenReturn(queryTaskEntity);
        return queryTaskEntity;
    }

    private static CloudTaskAssignedEventImpl taskAssignedEvent(String assignee) {
        boolean assigned = assignee != null && !assignee.isEmpty();
        TaskImpl task = new TaskImpl(TASK_ID, "task", assigned ? Task.TaskStatus.ASSIGNED : Task.TaskStatus.CREATED);
        task.setAssignee(assignee);
        return new CloudTaskAssignedEventImpl(task);
    }
}
