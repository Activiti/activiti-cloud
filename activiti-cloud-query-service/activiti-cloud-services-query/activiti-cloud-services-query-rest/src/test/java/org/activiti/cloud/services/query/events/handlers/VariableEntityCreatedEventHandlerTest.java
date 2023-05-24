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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
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
public class VariableEntityCreatedEventHandlerTest {

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

    @Test
    public void handleShouldCreateAndStoreProcessInstanceVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());
        event.setVariableDefinitionId("variableDefId");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        when(entityManagerFinder.findProcessInstanceWithVariables(event.getEntity().getProcessInstanceId()))
            .thenReturn(Optional.of(processInstanceEntity));

        //when
        processVariableCreatedEventHandler.handle(event);

        //then
        ArgumentCaptor<ProcessVariableEntity> captor = ArgumentCaptor.forClass(ProcessVariableEntity.class);
        verify(entityManager).persist(captor.capture());

        ProcessVariableEntity variableEntity = captor.getValue();

        Assertions
            .assertThat(variableEntity)
            .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
            .hasName(event.getEntity().getName())
            .hasTaskId(event.getEntity().getTaskId())
            .hasType(event.getEntity().getType())
            .isNotTaskVariable()
            .hasProcessInstance(processInstanceEntity)
            .hasVariableDefinitionId("variableDefId");
    }

    private static VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var", "string", "v1", "procInstId", null);
    }

    @Test
    public void handleShouldCreateAndStoreTaskVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariableWithTaskId());

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();

        when(entityManager.getReference(ProcessInstanceEntity.class, event.getEntity().getProcessInstanceId()))
            .thenReturn(processInstanceEntity);

        TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManagerFinder.findTaskWithVariables("taskId")).thenReturn(Optional.of(taskEntity));
        //when
        taskVariableCreatedEventHandler.handle(event);

        //then
        ArgumentCaptor<TaskVariableEntity> captor = ArgumentCaptor.forClass(TaskVariableEntity.class);
        verify(entityManager).persist(captor.capture());

        TaskVariableEntity variableEntity = captor.getValue();

        Assertions
            .assertThat(variableEntity)
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
    public void getHandledEventShouldReturnVariableCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
    }
}
