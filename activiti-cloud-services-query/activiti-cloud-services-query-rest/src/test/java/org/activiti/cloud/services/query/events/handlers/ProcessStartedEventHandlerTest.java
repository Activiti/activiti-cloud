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

import java.util.Optional;
import java.util.UUID;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessStartedEventHandlerTest {

    @InjectMocks
    private ProcessStartedEventHandler handler;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateProcessInstanceStatusToRunningAndUpdateInstanceName() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        given(currentProcessInstanceEntity.getStatus()).willReturn(ProcessInstance.ProcessInstanceStatus.CREATED);
        given(processInstanceRepository.findById(event.getEntity().getId())).willReturn(Optional.of(currentProcessInstanceEntity));

        //when
        handler.handle(event);

        //then
        verify(processInstanceRepository).save(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity).setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        verify(currentProcessInstanceEntity).setName(event.getEntity().getName());
    }

    @Test
    public void handleShouldIgnoreEventIfProcessInstanceIsAlreadyInRunningStatus() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstanceEntity currentProcessInstanceEntity = mock(ProcessInstanceEntity.class);
        given(currentProcessInstanceEntity.getStatus()).willReturn(ProcessInstance.ProcessInstanceStatus.RUNNING);
        given(processInstanceRepository.findById(event.getEntity().getId())).willReturn(Optional.of(currentProcessInstanceEntity));

        //when
        handler.handle(event);

        //then
        verify(processInstanceRepository,
               never()).save(currentProcessInstanceEntity);
        verify(currentProcessInstanceEntity,
               never()).setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
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

        given(processInstanceRepository.findById("200")).willReturn(Optional.empty());

        //then
        expectedException.expect(QueryException.class);
        expectedException.expectMessage("Unable to find process instance with the given id: ");

        //when
        handler.handle(event);
    }

    @Test
    public void getHandledEventShouldReturnProcessStartedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name());
    }
}