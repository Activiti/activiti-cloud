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

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.engine.ActivitiException;
import org.activiti.runtime.api.event.CloudProcessResumedEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessResumedEventImpl;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessResumedEventHandlerTest {

    @InjectMocks
    private ProcessResumedEventHandler handler;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateCurrentProcessInstanceStateToRunning() {
        //given
        ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
        eventProcessInstance.setId(UUID.randomUUID().toString());
        CloudProcessResumedEvent event = new CloudProcessResumedEventImpl(eventProcessInstance);

        ProcessInstance currentProcessInstance = mock(ProcessInstance.class);
        given(processInstanceRepository.findById(eventProcessInstance.getId())).willReturn(Optional.of(currentProcessInstance));

        //when
        handler.handle(event);

        //then
        verify(processInstanceRepository).save(currentProcessInstance);
        verify(currentProcessInstance).setStatus(org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus.RUNNING.name());
        verify(currentProcessInstance).setLastModified(any(Date.class));
    }

    @Test
    public void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
        //given
        ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
        eventProcessInstance.setId(UUID.randomUUID().toString());
        CloudProcessResumedEvent event = new CloudProcessResumedEventImpl(eventProcessInstance);

        given(processInstanceRepository.findById(eventProcessInstance.getId())).willReturn(Optional.empty());

        //then
        expectedException.expect(ActivitiException.class);
        expectedException.expectMessage("Unable to find process instance with the given id: ");

        //when
        handler.handle(event);

    }

    @Test
    public void getHandledEventShouldReturnProcessResumedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name());
    }
}