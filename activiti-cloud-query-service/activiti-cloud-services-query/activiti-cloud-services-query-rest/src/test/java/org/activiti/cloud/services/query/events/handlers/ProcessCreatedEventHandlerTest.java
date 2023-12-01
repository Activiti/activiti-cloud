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
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.test.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessCreatedEventHandlerTest {

    @InjectMocks
    private ProcessCreatedEventHandler handler;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldUpdateCurrentProcessInstanceStateToCreated() {
        //given
        CloudProcessCreatedEvent event = buildProcessCreatedEvent();

        //when
        handler.handle(event);

        ArgumentCaptor<ProcessInstanceEntity> argumentCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(entityManager).persist(argumentCaptor.capture());

        ProcessInstanceEntity processInstanceEntity = argumentCaptor.getValue();
        Assertions
            .assertThat(processInstanceEntity)
            .hasId(event.getEntity().getId())
            .hasProcessDefinitionId(event.getEntity().getProcessDefinitionId())
            .hasServiceName(event.getServiceName())
            .hasProcessDefinitionKey(event.getEntity().getProcessDefinitionKey())
            .hasStatus(ProcessInstance.ProcessInstanceStatus.CREATED)
            .hasName(event.getEntity().getName())
            .hasProcessDefinitionName(event.getEntity().getProcessDefinitionName());
    }

    private CloudProcessCreatedEvent buildProcessCreatedEvent() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setProcessDefinitionId(UUID.randomUUID().toString());
        processInstance.setBusinessKey("myKey");
        processInstance.setName("myName");
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
