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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessVariableCreatedEventHandlerTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @InjectMocks
    private ProcessVariableCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handle_shouldCreateNewVariable_whenVariableDoesNotExist() {
        CloudVariableCreatedEvent event = mock(CloudVariableCreatedEvent.class);
        ProcessInstanceEntity processInstance = mock(ProcessInstanceEntity.class);
        when(event.getEntity().getProcessInstanceId()).thenReturn("processInstanceId");
        when(event.getEntity().getName()).thenReturn("variableName");
        when(entityManagerFinder.findProcessInstanceWithVariables("processInstanceId"))
            .thenReturn(Optional.of(processInstance));
        when(processInstance.getVariable("variableName")).thenReturn(Optional.empty());

        handler.handle(event);

        verify(entityManager).persist(any(ProcessVariableEntity.class));
    }

    @Test
    void handle_shouldNotCreateNewVariable_whenVariableAlreadyExists() {
        CloudVariableCreatedEvent event = mock(CloudVariableCreatedEvent.class);
        ProcessInstanceEntity processInstance = mock(ProcessInstanceEntity.class);
        ProcessVariableEntity existingVariable = mock(ProcessVariableEntity.class);
        when(event.getEntity().getProcessInstanceId()).thenReturn("processInstanceId");
        when(event.getEntity().getName()).thenReturn("variableName");
        when(entityManagerFinder.findProcessInstanceWithVariables("processInstanceId"))
            .thenReturn(Optional.of(processInstance));
        when(processInstance.getVariable("variableName")).thenReturn(Optional.of(existingVariable));

        handler.handle(event);

        verify(entityManager, never()).persist(any(ProcessVariableEntity.class));
    }

    @Test
    void handle_shouldWarn_whenVariableAlreadyExists() {
        CloudVariableCreatedEvent event = mock(CloudVariableCreatedEvent.class);
        ProcessInstanceEntity processInstance = mock(ProcessInstanceEntity.class);
        ProcessVariableEntity existingVariable = mock(ProcessVariableEntity.class);
        when(event.getEntity().getProcessInstanceId()).thenReturn("processInstanceId");
        when(event.getEntity().getName()).thenReturn("variableName");
        when(entityManagerFinder.findProcessInstanceWithVariables("processInstanceId"))
            .thenReturn(Optional.of(processInstance));
        when(processInstance.getVariable("variableName")).thenReturn(Optional.of(existingVariable));

        handler.handle(event);

        verify(entityManager, never()).persist(any(ProcessVariableEntity.class));
    }

    @Test
    void handle_shouldDoNothing_whenProcessInstanceNotFound() {
        CloudVariableCreatedEvent event = mock(CloudVariableCreatedEvent.class);
        when(event.getEntity().getProcessInstanceId()).thenReturn("processInstanceId");
        when(entityManagerFinder.findProcessInstanceWithVariables("processInstanceId")).thenReturn(Optional.empty());

        handler.handle(event);

        verify(entityManager, never()).persist(any(ProcessVariableEntity.class));
    }
}
