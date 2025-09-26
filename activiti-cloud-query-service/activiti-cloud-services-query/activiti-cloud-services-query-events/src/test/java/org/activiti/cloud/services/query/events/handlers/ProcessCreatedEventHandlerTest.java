/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
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
    EntityManager entityManager;

    @Test
    public void handleShouldCreateAndStoreProcessInstanceEntity() {
        //given
        CloudProcessCreatedEvent event = new CloudProcessCreatedEventImpl(buildProcess());

        when(entityManager.find(ProcessInstanceEntity.class, event.getEntity().getId())).thenReturn(null);
        handler.handle(event);

        //then
        ArgumentCaptor<ProcessInstanceEntity> captor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(entityManager).persist(captor.capture());

        ProcessInstanceEntity processInstanceEntity = captor.getValue();

        var expectedEventEntity = event.getEntity();

        assertThat(processInstanceEntity.getId()).isEqualTo(expectedEventEntity.getId());
        assertThat(processInstanceEntity.getName()).isEqualTo(expectedEventEntity.getName());
        assertThat(processInstanceEntity.getProcessDefinitionId())
            .isEqualTo(expectedEventEntity.getProcessDefinitionId());
        assertThat(processInstanceEntity.getRootProcessInstanceId())
            .isEqualTo(expectedEventEntity.getRootProcessInstanceId());
    }

    private static ProcessInstanceImpl buildProcess() {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("id");
        processInstance.setName("name");
        processInstance.setProcessDefinitionId("processDefinitionId");
        processInstance.setRootProcessInstanceId("rootProcessInstanceId");

        return processInstance;
    }

    @Test
    public void getHandledEventShouldReturnProcessCreatedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name());
    }
}
