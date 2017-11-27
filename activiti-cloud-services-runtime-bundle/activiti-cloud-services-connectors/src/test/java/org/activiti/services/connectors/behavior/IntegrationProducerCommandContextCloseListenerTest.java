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

package org.activiti.services.connectors.behavior;

import java.util.Arrays;
import java.util.Collections;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEvent;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrationProducerCommandContextCloseListenerTest {

    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String INTEGRATION_CONTEXT_ID = "intContextId";
    private static final String APP_NAME = "myApp";

    @InjectMocks
    private IntegrationProducerCommandContextCloseListener closeListener;

    @Mock
    private ProcessEngineIntegrationChannels integrationChannels;

    @Mock
    private MessageChannel integrationProducerChannel;

    @Mock
    private ProcessEngineChannels processEngineChannels;

    @Mock
    private MessageChannel audiProducerChannel;

    @Mock
    private CommandContext commandContext;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Mock
    private Message<IntegrationRequestEvent> firstIntegration;

    @Mock
    private Message<IntegrationRequestEvent> secondIntegration;

    @Captor
    private ArgumentCaptor<Message<ProcessEngineEvent[]>> messageArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(integrationChannels.integrationEventsProducer()).thenReturn(integrationProducerChannel);
        when(processEngineChannels.auditProducer()).thenReturn(audiProducerChannel);
        when(runtimeBundleProperties.getEventsProperties()).thenReturn(eventsProperties);
    }

    @Test
    public void closedShouldSendRegisteredMessages() throws Exception {
        //given
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(Arrays.asList(firstIntegration,
                                          secondIntegration));

        //when
        closeListener.closed(commandContext);

        //then
        verify(integrationProducerChannel).send(firstIntegration);
        verify(integrationProducerChannel).send(secondIntegration);
    }

    @Test
    public void closedShouldDoNothingWhenMessagesIsNull() throws Exception {
        //given
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(null);

        //when
        closeListener.closed(commandContext);

        //then
        verify(integrationProducerChannel,
               never()).send(any());
    }

    @Test
    public void closedShouldDoNothingWhenMessagesIsEmpty() throws Exception {
        //given
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(Collections.emptyList());

        //when
        closeListener.closed(commandContext);

        //then
        verify(integrationProducerChannel,
               never()).send(any());
    }

    @Test
    public void executeShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() throws Exception {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);
        given(runtimeBundleProperties.getName()).willReturn(APP_NAME);

        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(Collections.singletonList(firstIntegration));
        given(firstIntegration.getPayload()).willReturn(new IntegrationRequestEvent(PROC_INST_ID,
                                                                                    PROC_DEF_ID,
                                                                                    EXECUTION_ID,
                                                                                    INTEGRATION_CONTEXT_ID,
                                                                                    null));

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        //when
        closeListener.closed(commandContext);

        //then
        verify(audiProducerChannel).send(messageArgumentCaptor.capture());

        Message<ProcessEngineEvent[]> message = messageArgumentCaptor.getValue();
        assertThat(message.getPayload()).hasSize(1);
        assertThat(message.getPayload()[0]).isInstanceOf(IntegrationRequestSentEvent.class);

        IntegrationRequestSentEvent integrationRequestSentEvent = (IntegrationRequestSentEvent) message.getPayload()[0];

        assertThat(integrationRequestSentEvent.getIntegrationContextId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequestSentEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequestSentEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(integrationRequestSentEvent.getApplicationName()).isEqualTo(APP_NAME);
    }

    @Test
    public void executeShouldNotSendIntegrationAuditEventWhenIntegrationAuditEventsAreDisabled() throws Exception {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);
        given(runtimeBundleProperties.getName()).willReturn(APP_NAME);

        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(Collections.singletonList(firstIntegration));


        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        //when
        closeListener.closed(commandContext);

        //then
        verify(audiProducerChannel,
               never()).send(ArgumentMatchers.any());
    }

}