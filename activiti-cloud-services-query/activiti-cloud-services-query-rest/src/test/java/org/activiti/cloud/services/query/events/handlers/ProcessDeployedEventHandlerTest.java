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

import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessDeployedEventHandlerTest {

    @InjectMocks
    private ProcessDeployedEventHandler handler;

    @Mock
    private ProcessDefinitionRepository repository;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleShouldStoreProcessDefinition() {
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

        //when
        handler.handle(processDeployedEvent);

        //then
        ArgumentCaptor<ProcessDefinitionEntity> captor = ArgumentCaptor.forClass(ProcessDefinitionEntity.class);

        verify(repository).save(captor.capture());
        ProcessDefinitionEntity storedProcess = captor.getValue();
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
    }

    @Test
    public void getHandledEventShouldReturnProcessDeployedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        Assertions.assertThat(handledEvent).isEqualTo(ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED.name());
    }
}