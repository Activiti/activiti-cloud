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

import java.util.ArrayList;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEvent;
import org.activiti.cloud.services.events.configuration.ApplicationProperties;
import org.activiti.cloud.services.events.listeners.CommandContextEventsAggregator;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.messaging.Message;

import static org.activiti.services.test.DelegateExecutionBuilder.anExecution;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MQServiceTaskBehaviorTest {

    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";

    @Spy
    @InjectMocks
    private MQServiceTaskBehavior behavior;

    @Mock
    private ProcessEngineIntegrationChannels integrationChannels;
    private IntegrationContextManager integrationContextManager;

    @Mock
    private IntegrationProducerCommandContextCloseListener contextCloseListener;

    @Mock
    private CommandContext commandContext;
    private MessageChannel integrationRequestMessageChannel;

    @Mock
    private CommandContextEventsAggregator eventsAggregator;

    @Mock
    private ApplicationProperties applicationProperties;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequestEvent>> integrationRequestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(behavior.getCurrentCommandContext()).thenReturn(commandContext);
        when(integrationChannels.integrationEventsProducer()).thenReturn(integrationRequestMessageChannel);
    }

    @Test
    public void executeShouldStoreTheIntegrationContextAndSendAMessage() throws Exception {
        //given
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        DelegateExecution execution = anExecution()
                .withId(EXECUTION_ID)
                .withProcessInstanceId(PROC_INST_ID)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withServiceTask(serviceTask)
                .build();
        given(applicationProperties.getName()).willReturn("myApp");

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setId("entityId");
        given(integrationContextManager.create()).willReturn(entity);

        ArrayList<Message<IntegrationRequestEvent>> messages = new ArrayList<>();
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS))
                .willReturn(messages);

        //when
        behavior.execute(execution);

        //then
        verify(integrationContextManager).insert(entity);
        assertThat(entity.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(entity.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(entity.getProcessInstanceId()).isEqualTo(PROC_INST_ID);

        assertThat(messages).hasSize(1);
        Message<IntegrationRequestEvent> message = messages.get(0);
        assertThat(message.getPayload().getExecutionId()).isNotNull();
        assertThat(message.getPayload().getProcessInstanceId()).isEqualTo("procInstId");
        assertThat(message.getPayload().getProcessDefinitionId()).isEqualTo("procDefId");
        verify(integrationRequestMessageChannel).send(integrationRequestCaptor.capture());
        Message<IntegrationRequestEvent> message = integrationRequestCaptor.getValue();
        assertThat(message.getPayload().getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(message.getPayload().getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(message.getPayload().getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(message.getHeaders().get("connectorType")).isEqualTo(CONNECTOR_TYPE);

    }

    @Test
    public void executeShouldRegisterIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() throws Exception {
        //given
        given(applicationProperties.isIntegrationAuditEventsEnabled()).willReturn(true);
        given(applicationProperties.getName()).willReturn("myApp");

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        DelegateExecution execution = anExecution()
                .withId(EXECUTION_ID)
                .withProcessInstanceId(PROC_INST_ID)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withServiceTask(serviceTask)
                .build();

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setId("entityId");
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(execution);

        //then
        ArgumentCaptor<ProcessEngineEvent> processEngineEventArgumentCaptor = ArgumentCaptor.forClass(ProcessEngineEvent.class);
        verify(eventsAggregator).add(processEngineEventArgumentCaptor.capture());

        assertThat(processEngineEventArgumentCaptor.getValue()).isInstanceOf(IntegrationRequestSentEvent.class);
        IntegrationRequestSentEvent integrationRequestSentEvent = (IntegrationRequestSentEvent) processEngineEventArgumentCaptor.getValue();
        assertThat(integrationRequestSentEvent.getIntegrationContextId()).isEqualTo("entityId");
        assertThat(integrationRequestSentEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequestSentEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(integrationRequestSentEvent.getApplicationName()).isEqualTo("myApp");
    }

    @Test
    public void executeShouldNotRegisterIntegrationAuditEventWhenIntegrationAuditEventsAreDisabled() throws Exception {
        //given
        given(applicationProperties.isIntegrationAuditEventsEnabled()).willReturn(false);
        given(applicationProperties.getName()).willReturn("myApp");

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        DelegateExecution execution = anExecution()
                .withId(EXECUTION_ID)
                .withServiceTask(serviceTask)
                .build();

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        entity.setId("entityId");
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(execution);

        //then
        verify(eventsAggregator, never()).add(any(ProcessEngineEvent.class));
    }

    @Test
    public void executeShouldRegisterCloseListenerWhenAbsent() throws Exception {
        //given
        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(false);
        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(anyExecution());

        //then
        verify(commandContext).addCloseListener(contextCloseListener);
    }

    @Test
    public void executeShouldNotRegisterCloseListenerWhenAlreadyPresent() throws Exception {
        //given

        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(true);

        IntegrationContextEntityImpl entity = new IntegrationContextEntityImpl();
        given(integrationContextManager.create()).willReturn(entity);

        //when
        behavior.execute(anyExecution());

        //then
        verify(commandContext, never()).addCloseListener(contextCloseListener);
    }

    private DelegateExecution anyExecution() {
        String connectorType = "payment";
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(connectorType);

        DelegateExecution execution = mock(DelegateExecution.class);
        given(execution.getId()).willReturn("execId");
        given(execution.getCurrentFlowElement()).willReturn(serviceTask);
        return execution;
    }

    @Test
    public void triggerShouldCallLeave() throws Exception {
        //given
        DelegateExecution execution = mock(DelegateExecution.class);
        doNothing().when(behavior).leave(execution);

        //when
        behavior.trigger(execution, null, null);

        //then
        verify(behavior).leave(execution);
    }
}