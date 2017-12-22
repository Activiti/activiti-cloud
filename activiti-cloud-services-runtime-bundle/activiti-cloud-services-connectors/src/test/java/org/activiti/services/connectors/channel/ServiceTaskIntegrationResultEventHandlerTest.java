/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.services.connectors.channel;

import java.util.Collections;
import java.util.Map;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationResultReceivedEvent;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.services.connectors.model.IntegrationResultEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServiceTaskIntegrationResultEventHandlerTest {

    private static final String EXECUTION_ID = "execId";
    private static final String ENTITY_ID = "entityId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";

    @InjectMocks
    private ServiceTaskIntegrationResultEventHandler handler;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Mock
    private ProcessEngineChannels channels;

    @Mock
    private MessageChannel auditChannel;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Captor
    private ArgumentCaptor<Message<IntegrationResultReceivedEvent[]>> messageCaptor;

    @Mock
    private ExecutionQuery executionQuery;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(channels.auditProducer()).thenReturn(auditChannel);
        when(runtimeBundleProperties.getEventsProperties()).thenReturn(eventsProperties);
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(anyString())).thenReturn(executionQuery);
        when(executionQuery.list()).thenReturn(Collections.emptyList());
    }

    @Test
    public void receiveShouldTriggerTheExecutionAndDeleteTheRelatedIntegrationContext() throws Exception {
        //given
        IntegrationContextEntityImpl integrationContext = new IntegrationContextEntityImpl();
        integrationContext.setExecutionId(EXECUTION_ID);
        integrationContext.setId(ENTITY_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);

        given(integrationContextService.findIntegrationContextByExecutionId(EXECUTION_ID))
                .willReturn(integrationContext);
        given(executionQuery.list()).willReturn(Collections.singletonList(mock(Execution.class)));
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        IntegrationResultEvent integrationResultEvent = new IntegrationResultEvent(EXECUTION_ID,
                                                                                   variables);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(integrationContextService).deleteIntegrationContext(integrationContext);
        verify(runtimeService).trigger(EXECUTION_ID,
                                       variables);
    }

    @Test
    public void receiveShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() throws Exception {
        //given
        IntegrationContextEntityImpl integrationContext = new IntegrationContextEntityImpl();
        integrationContext.setExecutionId(EXECUTION_ID);
        integrationContext.setId(ENTITY_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);

        given(integrationContextService.findIntegrationContextByExecutionId(EXECUTION_ID)).willReturn(integrationContext);
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        given(runtimeBundleProperties.getName()).willReturn("myApp");
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationResultEvent integrationResultEvent = new IntegrationResultEvent(EXECUTION_ID,
                                                                                   variables);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(auditChannel).send(messageCaptor.capture());
        Message<IntegrationResultReceivedEvent[]> message = messageCaptor.getValue();
        assertThat(message.getPayload()).hasSize(1);
        IntegrationResultReceivedEvent integrationResultReceivedEvent = message.getPayload()[0];
        assertThat(integrationResultReceivedEvent.getIntegrationContextId()).isEqualTo(ENTITY_ID);
        assertThat(integrationResultReceivedEvent.getApplicationName()).isEqualTo("myApp");
        assertThat(integrationResultReceivedEvent.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(integrationResultReceivedEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationResultReceivedEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
    }


    @Test
    public void retrieveShouldNotSentAuditEventWhenIntegrationAuditEventsAreDisabled() throws Exception {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);

        IntegrationContextEntityImpl integrationContext = new IntegrationContextEntityImpl();
        String executionId = "execId";

        given(integrationContextService.findIntegrationContextByExecutionId(executionId)).willReturn(integrationContext);
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        IntegrationResultEvent integrationResultEvent = new IntegrationResultEvent(executionId,
                                                                                   variables);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(auditChannel,
               never()).send(any(Message.class));
    }
}