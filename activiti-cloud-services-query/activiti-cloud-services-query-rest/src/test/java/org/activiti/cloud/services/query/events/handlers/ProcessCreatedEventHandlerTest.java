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

import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.runtime.api.event.CloudProcessCreated;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.impl.ProcessInstanceImpl;
import org.activiti.test.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessCreatedEventHandlerTest {

    @InjectMocks
    private ProcessCreatedEventHandler handler;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldUpdateCurrentProcessInstanceStateToCreated() {
        //given
        CloudProcessCreated event = buildProcessCreatedEvent();

        //when
        handler.handle(event);

        ArgumentCaptor<ProcessInstanceEntity> argumentCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(processInstanceRepository).save(argumentCaptor.capture());

        ProcessInstanceEntity processInstanceEntity = argumentCaptor.getValue();
        Assertions.assertThat(processInstanceEntity)
                .hasId(event.getEntity().getId())
                .hasProcessDefinitionId(event.getEntity().getProcessDefinitionId())
                .hasServiceName(event.getServiceName())
                .hasProcessDefinitionKey(event.getEntity().getProcessDefinitionKey())
                .hasStatus(ProcessInstance.ProcessInstanceStatus.CREATED);
    }

    private CloudProcessCreated buildProcessCreatedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setProcessDefinitionId(UUID.randomUUID().toString());
        processInstance.setBusinessKey("myKey");
        CloudProcessCreatedEventImpl event = new CloudProcessCreatedEventImpl(processInstance);
        event.setServiceName("runtime-bundle-a");
        return event;
    }

    @Test
    public void getHandledEventShouldReturnProcessCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name());
    }
}