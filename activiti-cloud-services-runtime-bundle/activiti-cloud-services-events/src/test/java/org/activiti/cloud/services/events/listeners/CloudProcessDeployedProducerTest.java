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

package org.activiti.cloud.services.events.listeners;

import java.util.Arrays;
import java.util.List;

import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.runtime.api.model.impl.APIProcessDefinitionConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CloudProcessDeployedProducerTest {

    @InjectMocks
    private CloudProcessDeployedProducer processDeployedProducer;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private APIProcessDefinitionConverter processDefinitionConverter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private ProcessEngineChannels producer;

    @Mock
    private MessageChannel auditProducer;

    @Captor
    private ArgumentCaptor<Message<CloudProcessDeployedEvent[]>> messageCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        when(producer.auditProducer()).thenReturn(auditProducer);
    }

    @Test
    public void shouldSendMessageWithDeployedProcessesWhenWebApplicationTypeIsServlet() {
        //given
        ProcessDefinitionQuery definitionQuery = mock(ProcessDefinitionQuery.class);
        given(repositoryService.createProcessDefinitionQuery()).willReturn(definitionQuery);

        List<ProcessDefinition> internalProcessDefinitions = Arrays.asList(mock(ProcessDefinition.class),
                                                                           mock(ProcessDefinition.class));

        given(definitionQuery.list()).willReturn(internalProcessDefinitions);

        List<org.activiti.api.process.model.ProcessDefinition> apiProcessDefinitions = Arrays.asList(mock(org.activiti.api.process.model.ProcessDefinition.class),
                                                                                                     mock(org.activiti.api.process.model.ProcessDefinition.class));
        given(processDefinitionConverter.from(internalProcessDefinitions)).willReturn(apiProcessDefinitions);

        //when
        processDeployedProducer.onApplicationEvent(buildApplicationReadyEvent(WebApplicationType.SERVLET));

        //then
        verify(runtimeBundleInfoAppender, times(2)).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
        verify(auditProducer).send(messageCaptor.capture());
        Message<CloudProcessDeployedEvent[]> message = messageCaptor.getValue();
        assertThat(message.getPayload())
                .extracting(CloudProcessDeployedEvent::getEntity)
                .containsExactlyElementsOf(apiProcessDefinitions);
    }

    private ApplicationReadyEvent buildApplicationReadyEvent(WebApplicationType applicationType) {
        ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);
        SpringApplication springApplication = mock(SpringApplication.class);
        given(springApplication.getWebApplicationType()).willReturn(applicationType);
        given(applicationReadyEvent.getSpringApplication()).willReturn(springApplication);
        return applicationReadyEvent;
    }

    @Test
    public void shouldNotSentMessageWhenWebApplicationTypeIsNone() {
        //when
        processDeployedProducer.onApplicationEvent(buildApplicationReadyEvent(WebApplicationType.NONE));

        //then
        verifyZeroInteractions(auditProducer);
    }

}