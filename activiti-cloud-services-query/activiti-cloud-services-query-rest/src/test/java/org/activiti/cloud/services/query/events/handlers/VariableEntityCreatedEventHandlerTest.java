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

import java.util.UUID;
import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.runtime.api.event.VariableEvent;
import org.activiti.runtime.api.event.impl.CloudVariableCreatedEventImpl;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;
import org.activiti.test.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableEntityCreatedEventHandlerTest {

    @InjectMocks
    private VariableCreatedEventHandler handler;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private EntityManager entityManager;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldCreateAndStoreProcessInstanceVariable() {
        //given
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(buildVariable());

        ProcessInstanceEntity processInstanceEntity = mock(ProcessInstanceEntity.class);
        when(entityManager.getReference(ProcessInstanceEntity.class,
                                        event.getEntity().getProcessInstanceId()))
                .thenReturn(processInstanceEntity);

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<VariableEntity> captor = ArgumentCaptor.forClass(VariableEntity.class);
        verify(variableRepository).save(captor.capture());

        VariableEntity variableEntity = captor.getValue();

        Assertions.assertThat(variableEntity)
                .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
                .hasName(event.getEntity().getName())
                .hasTaskId(event.getEntity().getTaskId())
                .hasType(event.getEntity().getType())
                .hasTask(null)
                .hasProcessInstance(processInstanceEntity);
    }

    @Test
    public void handleShouldCreateAndStoreTaskVariable() {
        //given
        VariableInstanceImpl<String> variableInstance = buildVariable();
        variableInstance.setTaskId(UUID.randomUUID().toString());
        CloudVariableCreatedEventImpl event = new CloudVariableCreatedEventImpl(variableInstance);

        ProcessInstanceEntity processInstanceEntity = mock(ProcessInstanceEntity.class);
        when(entityManager.getReference(ProcessInstanceEntity.class,
                                        event.getEntity().getProcessInstanceId()))
                .thenReturn(processInstanceEntity);

        TaskEntity taskEntity = mock(TaskEntity.class);
        when(entityManager.getReference(TaskEntity.class,
                                        event.getEntity().getTaskId()))
                .thenReturn(taskEntity);

        //when
        handler.handle(event);

        //then
        ArgumentCaptor<VariableEntity> captor = ArgumentCaptor.forClass(VariableEntity.class);
        verify(variableRepository).save(captor.capture());

        VariableEntity variableEntity = captor.getValue();

        Assertions.assertThat(variableEntity)
                .hasProcessInstanceId(event.getEntity().getProcessInstanceId())
                .hasName(event.getEntity().getName())
                .hasTaskId(event.getEntity().getTaskId())
                .hasType(event.getEntity().getType())
                .hasTask(taskEntity)
                .hasProcessInstance(processInstanceEntity);
    }

    private VariableInstanceImpl<String> buildVariable() {
        return new VariableInstanceImpl<>("var",
                                          "string",
                                          "v1",
                                          UUID.randomUUID().toString());
    }

    @Test
    public void getHandledEventShouldReturnVariableCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(VariableEvent.VariableEvents.VARIABLE_CREATED.name());
    }
}