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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessStartedEventHandlerTest {

    @InjectMocks
    private ProcessStartedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldUpdateProcessInstanceStatusToRunningAndUpdateInstanceName() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        given(currentProcessInstanceEntity.getStatus()).willReturn(ProcessInstance.ProcessInstanceStatus.CREATED);
        given(entityManager.find(ProcessInstanceEntity.class, event.getEntity().getId()))
            .willReturn(currentProcessInstanceEntity);
        //when
        handler.handle(event);

        //then
        verify(entityManager).persist(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity).setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        verify(currentProcessInstanceEntity).setName(event.getEntity().getName());
        verify(currentProcessInstanceEntity).setStartDate(event.getEntity().getStartDate());
    }

    @Test
    public void handleShouldIgnoreEventIfProcessInstanceIsAlreadyInRunningStatus() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        given(currentProcessInstanceEntity.getStatus()).willReturn(ProcessInstance.ProcessInstanceStatus.RUNNING);
        given(entityManager.find(ProcessInstanceEntity.class, event.getEntity().getId()))
            .willReturn(currentProcessInstanceEntity);
        //when
        handler.handle(event);

        //then
        verify(entityManager, never()).persist(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity, never()).setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
    }

    private CloudProcessStartedEvent buildProcessStartedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setName("my instance name");
        return new CloudProcessStartedEventImpl(processInstance);
    }

    @Test
    public void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();

        given(entityManager.find(ProcessInstanceEntity.class, event.getProcessInstanceId())).willReturn(null);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessageContaining("Unable to find process instance with the given id: ");
    }

    @Test
    public void getHandledEventShouldReturnProcessStartedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name());
    }
}
