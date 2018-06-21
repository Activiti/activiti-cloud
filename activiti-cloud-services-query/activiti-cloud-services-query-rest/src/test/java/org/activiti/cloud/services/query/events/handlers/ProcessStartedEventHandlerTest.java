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

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.engine.ActivitiException;
import org.activiti.runtime.api.event.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
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
    public void handleShouldUpdateProcessInstanceStatusToRunning() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstance currentProcessInstance = mock(ProcessInstance.class);
        given(currentProcessInstance.getStatus()).willReturn(org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus.CREATED.name());
        given(processInstanceRepository.findById(event.getEntity().getId())).willReturn(Optional.of(currentProcessInstance));

        //when
        handler.handle(event);

        //then
        verify(processInstanceRepository).save(currentProcessInstance);
        verify(currentProcessInstance).setStatus(org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus.RUNNING.name());
    }

    @Test
    public void handleShouldIgnoreEventIfProcessInstanceIsAlreadyInRunningStatus() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();
        ProcessInstance currentProcessInstance = mock(ProcessInstance.class);
        given(currentProcessInstance.getStatus()).willReturn(org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus.RUNNING.name());
        given(processInstanceRepository.findById(event.getEntity().getId())).willReturn(Optional.of(currentProcessInstance));

        //when
        handler.handle(event);

        //then
        verify(processInstanceRepository, never()).save(currentProcessInstance);
        verify(currentProcessInstance, never()).setStatus(org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus.RUNNING.name());
    }

    private CloudProcessStartedEvent buildProcessStartedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        return new CloudProcessStartedEventImpl(processInstance);
    }

    @Test
    public void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
        //given
        CloudProcessStartedEvent event = buildProcessStartedEvent();

        given(processInstanceRepository.findById("200")).willReturn(Optional.empty());

        //then
        expectedException.expect(ActivitiException.class);
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