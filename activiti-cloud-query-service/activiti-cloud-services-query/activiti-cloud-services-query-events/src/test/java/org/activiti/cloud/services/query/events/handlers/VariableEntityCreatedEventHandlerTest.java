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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.test.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariableEntityCreatedEventHandlerTest {

    @InjectMocks
    private VariableCreatedEventHandler handler;

    @InjectMocks
    private ProcessVariableCreatedEventHandler processVariableCreatedEventHandler;

    @InjectMocks
    private TaskVariableCreatedEventHandler taskVariableCreatedEventHandler;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    // Used by VariableCreatedEventHandler routing tests; also injected into `handler` by Mockito
    @Mock
    private ProcessVariableCreatedEventHandler processVariableCreatedEventHandlerMock;

    @Mock
    private TaskVariableCreatedEventHandler taskVariableCreatedEventHandlerMock;

    @Test
    void handleShouldCreateAndStoreProcessInstanceVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());
        event.setVariableDefinitionId("variableDefId");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables(event.getEntity().getProcessInstanceId())).thenReturn(
            Optional.of(processInstanceEntity)
        );

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager).persist(captor.capture());
        List<Object> persisted = captor.getAllValues();

        ProcessVariableEntity variableEntity = (ProcessVariableEntity) persisted.get(0);
        Assertions.assertThat(variableEntity)
            .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
            .hasName(event.getEntity().getName())
            .hasTaskId(event.getEntity().getTaskId())
            .hasType(event.getEntity().getType())
            .isNotTaskVariable()
            .hasProcessInstance(processInstanceEntity)
            .hasVariableDefinitionId("variableDefId");
    }

    @Test
    void handleShouldCreateVariableWhenVariableIsEphemeral() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable(), true);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables(event.getEntity().getProcessInstanceId())).thenReturn(
            Optional.of(processInstanceEntity)
        );

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(entityManager, times(1)).persist(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ProcessVariableEntity.class);
    }

    private static VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var", "string", "v1", "procInstId", null);
    }

    @Test
    void handleShouldCreateAndStoreTaskVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariableWithTaskId());

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();

        when(
            entityManager.getReference(ProcessInstanceEntity.class, event.getEntity().getProcessInstanceId())
        ).thenReturn(processInstanceEntity);

        TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.of(taskEntity));
        //when
        taskVariableCreatedEventHandler.handle(event);

        //then
        ArgumentCaptor<TaskVariableEntity> captor = ArgumentCaptor.forClass(TaskVariableEntity.class);
        verify(entityManager).persist(captor.capture());

        TaskVariableEntity variableEntity = captor.getValue();

        Assertions.assertThat(variableEntity)
            .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
            .hasName(event.getEntity().getName())
            .hasTaskId(event.getEntity().getTaskId())
            .hasType(event.getEntity().getType())
            .hasTask(taskEntity)
            .hasProcessInstance(processInstanceEntity);
    }

    private static VariableInstanceImpl<String> buildVariableWithTaskId() {
        return new VariableInstanceImpl<>("var", "string", "v1", "procInstId", "taskId");
    }

    @Test
    void getHandledEventShouldReturnVariableCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
    }

    @Test
    void handleShouldSkipWhenProcessInstanceNotFound() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(Optional.empty());

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        verifyNoInteractions(entityManager);
    }

    @Test
    void handleShouldWarnAndSkipWhenVariableAlreadyExistsInProcessInstance() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());

        ProcessVariableEntity existing = new ProcessVariableEntity();
        existing.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.getVariables().add(existing);

        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(
            Optional.of(processInstanceEntity)
        );

        //when
        processVariableCreatedEventHandler.handle(event);

        //then - no persist because variable already exists
        verifyNoInteractions(entityManager);
    }

    @Test
    void handleShouldAssignNewVariableToTask() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(
            Optional.of(processInstanceEntity)
        );

        TaskEntity taskEntity = new TaskEntity();
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of(taskEntity));

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        verify(entityManager).persist(any());
        assertThat(taskEntity.getProcessVariables()).extracting(ProcessVariableEntity::getName).containsExactly("var");
    }

    @Test
    void handleShouldWarnAndSkipWhenVariableAlreadyExistsInTask() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());

        ProcessVariableEntity existingInTask = new ProcessVariableEntity();
        existingInTask.setName("var");

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.getProcessVariables().add(existingInTask);

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables("procInstId")).thenReturn(
            Optional.of(processInstanceEntity)
        );
        when(entityManagerFinder.findTasksWithProcessVariables("procInstId")).thenReturn(Set.of(taskEntity));

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        verify(entityManager).persist(any());
        assertThat(taskEntity.getProcessVariables()).containsExactly(existingInTask);
    }

    @Test
    void handleShouldRouteToProcessVariableHandlerWhenNotTaskVariable() {
        //given - buildVariable has no taskId → isTaskVariable() false
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());

        //when
        handler.handle(event);

        //then
        verify(processVariableCreatedEventHandlerMock).handle(event);
        verifyNoInteractions(taskVariableCreatedEventHandlerMock);
    }

    @Test
    void handleShouldRouteToTaskVariableHandlerWhenTaskVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariableWithTaskId());

        //when
        handler.handle(event);

        //then
        verify(taskVariableCreatedEventHandlerMock).handle(event);
        verifyNoInteractions(processVariableCreatedEventHandlerMock);
    }

    @Test
    void handleShouldCatchAndLogExceptionFromSubHandler() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());
        doThrow(new RuntimeException("DB failure")).when(processVariableCreatedEventHandlerMock).handle(any());

        //when - must not propagate
        handler.handle(event);

        //then - sub-handler was invoked; exception was swallowed, not rethrown
        verify(processVariableCreatedEventHandlerMock).handle(event);
        verifyNoInteractions(taskVariableCreatedEventHandlerMock);
    }

    @Test
    void handleShouldThrowWhenTaskNotFound() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariableWithTaskId());
        when(entityManager.getReference(ProcessInstanceEntity.class, "procInstId")).thenReturn(
            new ProcessInstanceEntity()
        );
        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.empty());

        //when / then
        assertThatThrownBy(() -> taskVariableCreatedEventHandler.handle(event))
            .isInstanceOf(QueryException.class)
            .hasMessageContaining("taskId");
    }

    @Test
    void handleShouldWarnAndSkipWhenTaskVariableAlreadyExists() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariableWithTaskId());

        TaskVariableEntity existingVar = new TaskVariableEntity();
        existingVar.setName("var");

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.getVariables().add(existingVar);

        when(entityManager.getReference(ProcessInstanceEntity.class, "procInstId")).thenReturn(
            new ProcessInstanceEntity()
        );
        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.of(taskEntity));

        //when
        taskVariableCreatedEventHandler.handle(event);

        //then - no new variable persisted
        verify(entityManager, never()).persist(any());
    }

    @Test
    void handleShouldHandleNullProcessInstanceId() {
        //given - task variable with no processInstanceId
        VariableInstanceImpl<String> variable = new VariableInstanceImpl<>("var", "string", "v1", null, "taskId");
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(variable);

        TaskEntity taskEntity = mock(TaskEntity.class);
        when(taskEntity.getVariable("var")).thenReturn(Optional.empty());
        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.of(taskEntity));

        //when
        taskVariableCreatedEventHandler.handle(event);

        //then - getReference never called since processInstanceId was null
        verify(entityManager, never()).getReference(any(), any());
        verify(entityManager).persist(any(TaskVariableEntity.class));
    }
}
