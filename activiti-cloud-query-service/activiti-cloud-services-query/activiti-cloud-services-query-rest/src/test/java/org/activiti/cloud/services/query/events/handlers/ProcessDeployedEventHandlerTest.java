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

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessDeployedEventHandlerTest {

    @InjectMocks
    private ProcessDeployedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldStoreProcessDefinitionAndProcessModel() {
        //given
        ProcessDefinitionImpl eventProcess = new ProcessDefinitionImpl();
        eventProcess.setId(UUID.randomUUID().toString());
        eventProcess.setKey("myProcess");
        eventProcess.setName("My Process");
        eventProcess.setDescription("This is my process description");
        eventProcess.setFormKey("formKey");
        eventProcess.setVersion(2);

        CloudProcessDeployedEventImpl processDeployedEvent = new CloudProcessDeployedEventImpl(eventProcess);
        processDeployedEvent.setAppName("myApp");
        processDeployedEvent.setAppVersion("2.1");
        processDeployedEvent.setServiceFullName("my.full.service.name");
        processDeployedEvent.setServiceName("name");
        processDeployedEvent.setServiceType("runtime-bundle");
        processDeployedEvent.setServiceVersion("1.0");
        processDeployedEvent.setProcessModelContent("<model/>");

        //when
        handler.handle(processDeployedEvent);

        //then
        ArgumentCaptor<Object> argumentsCaptor = ArgumentCaptor.forClass(Object.class);

        verify(entityManager, times(2)).merge(argumentsCaptor.capture());

        ProcessDefinitionEntity storedProcess = (ProcessDefinitionEntity) argumentsCaptor.getAllValues().get(0);
        assertThat(storedProcess)
            .hasId(eventProcess.getId())
            .hasKey(eventProcess.getKey())
            .hasName(eventProcess.getName())
            .hasDescription(eventProcess.getDescription())
            .hasFormKey(eventProcess.getFormKey())
            .hasVersion(eventProcess.getVersion())
            .hasAppName(processDeployedEvent.getAppName())
            .hasAppVersion(processDeployedEvent.getAppVersion())
            .hasServiceFullName(processDeployedEvent.getServiceFullName())
            .hasServiceName(processDeployedEvent.getServiceName())
            .hasServiceType(processDeployedEvent.getServiceType())
            .hasServiceVersion(processDeployedEvent.getServiceVersion());

        ProcessModelEntity processModel = (ProcessModelEntity) argumentsCaptor.getAllValues().get(1);
        assertThat(processModel).hasProcessModelContent("<model/>");
    }

    @Test
    public void getHandledEventShouldReturnProcessDeployedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        Assertions
            .assertThat(handledEvent)
            .isEqualTo(ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name());
    }
}
