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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.task.model.Task.TaskStatus;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessCompletedEventHandlerTest {

    @InjectMocks
    private ProcessCompletedEventHandler handler;

    @Mock
    private TaskCancelledEventHandler taskCancelledEventHandler;

    @Mock
    private EntityManager entityManager;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent<?, ?>> cancelledEventArgumentCaptor;

    @Test
    void handleShouldUpdateCurrentProcessInstanceStateToCompleted() {
        //given
        CloudProcessCompletedEvent event = createProcessCompletedEvent();

        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        given(entityManager.find(ProcessInstanceEntity.class, event.getEntity().getId()))
            .willReturn(currentProcessInstanceEntity);

        //when
        handler.handle(event);

        //then
        verify(entityManager).persist(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity).setStatus(ProcessInstance.ProcessInstanceStatus.COMPLETED);
        verify(currentProcessInstanceEntity).setLastModified(any(Date.class));
    }

    private CloudProcessCompletedEvent createProcessCompletedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        return new CloudProcessCompletedEventImpl(processInstance);
    }

    @Test
    void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
        //given
        CloudProcessCompletedEvent event = createProcessCompletedEvent();
        given(entityManager.find(ProcessInstanceEntity.class, event.getProcessInstanceId())).willReturn(null);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessageContaining("Unable to find process instance with the given id: ");
    }

    @Test
    void getHandledEventShouldReturnProcessCompletedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name());
    }

    @Test
    void handleShouldUpdateAssignedAndCreatedChildTasksAsCancelled() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        CloudProcessCompletedEvent event = new CloudProcessCompletedEventImpl(processInstance);

        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        TaskEntity createdTask = getTaskMock(TaskStatus.CREATED);
        TaskEntity assignedTask = getTaskMock(TaskStatus.ASSIGNED);
        TaskEntity suspendedTask = getTaskMock(TaskStatus.SUSPENDED);
        when(currentProcessInstanceEntity.getTasks()).thenReturn(Set.of(createdTask, assignedTask, suspendedTask));
        given(entityManager.find(ProcessInstanceEntity.class, event.getEntity().getId()))
            .willReturn(currentProcessInstanceEntity);

        //when
        handler.handle(event);

        //then
        verify(currentProcessInstanceEntity).setLastModified(any(Date.class));
        verify(entityManager).persist(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity).setStatus(ProcessInstance.ProcessInstanceStatus.COMPLETED);

        verify(taskCancelledEventHandler, times(2)).handle(cancelledEventArgumentCaptor.capture());

        List<String> cancelledEntityIDs = cancelledEventArgumentCaptor
            .getAllValues()
            .stream()
            .map(CloudRuntimeEvent::getEntityId)
            .collect(Collectors.toList());

        assertThat(cancelledEntityIDs).containsExactlyInAnyOrder(createdTask.getId(), assignedTask.getId());
    }

    @NotNull
    private static TaskEntity getTaskMock(TaskStatus status) {
        TaskEntity mockTask = mock(TaskEntity.class);
        when(mockTask.getStatus()).thenReturn(status);
        lenient().when(mockTask.getId()).thenReturn(RandomStringUtils.random(1));
        return mockTask;
    }
}
