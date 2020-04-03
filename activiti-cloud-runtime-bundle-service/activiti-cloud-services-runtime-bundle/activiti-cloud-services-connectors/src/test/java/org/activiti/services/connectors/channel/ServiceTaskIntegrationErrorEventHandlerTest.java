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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Map;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.runtime.api.impl.VariablesMappingProvider;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class ServiceTaskIntegrationErrorEventHandlerTest {

    private static final String EXECUTION_ID = "execId";
    private static final String ENTITY_ID = "entityId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String CLIENT_ID = "entityId";
    private static final String CLIENT_NAME = "serviceTaskName";
    private static final String CLIENT_TYPE = ServiceTask.class.getSimpleName();

    @InjectMocks
    private ServiceTaskIntegrationErrorEventHandler handler;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Mock
    private MessageChannel auditProducer;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private VariablesMappingProvider outboundVariablesProvider;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Mock
    private IntegrationContextMessageBuilderFactory messageBuilderFactory;

    @Captor
    private ArgumentCaptor<Message<CloudRuntimeEvent<?, ?>[]>> messageCaptor;

    @Mock
    private ExecutionQuery executionQuery;

    @Before
    public void setUp() {
        initMocks(this);
        when(runtimeBundleProperties.getEventsProperties()).thenReturn(eventsProperties);
        when(runtimeBundleProperties.getServiceFullName()).thenReturn("myApp");
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(anyString())).thenReturn(executionQuery);
        when(executionQuery.list()).thenReturn(Collections.emptyList());
        when(messageBuilderFactory.create(ArgumentMatchers.any())).thenReturn(new MessageBuilderAppenderChain());
    }

    @Test
    public void receiveShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        integrationContextEntity.setProcessInstanceId(PROC_INST_ID);
        integrationContextEntity.setProcessDefinitionId(PROC_DEF_ID);

        given(integrationContextService.findById(ENTITY_ID)).willReturn(integrationContextEntity);
        Map<String, Object> variables = Collections.singletonMap("var1",
                "v");

        given(runtimeBundleProperties.getServiceFullName()).willReturn("myApp");
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(new IntegrationRequestImpl(integrationContext),
                                                                           new Error("Test Error"));

        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(auditProducer).send(messageCaptor.capture());
        Message<CloudRuntimeEvent<?, ?>[]> message = messageCaptor.getValue();
        CloudIntegrationErrorReceivedEventImpl event = (CloudIntegrationErrorReceivedEventImpl) message.getPayload()[0];
        assertThat(event.getEntity().getId()).isEqualTo(ENTITY_ID);
        assertThat(event.getEntity().getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(event.getEntity().getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);


        assertThat(event.getEntity().getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getEntity().getClientName()).isEqualTo(CLIENT_NAME);
        assertThat(event.getEntity().getClientType()).isEqualTo(CLIENT_TYPE);

        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(event);
    }

    private IntegrationContextImpl buildIntegrationContext(Map<String, Object> variables) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(ENTITY_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.addOutBoundVariables(variables);
        integrationContext.setClientId(CLIENT_ID);
        integrationContext.setClientName(CLIENT_NAME);
        integrationContext.setClientType(CLIENT_TYPE);

        return integrationContext;
    }

    @Test
    public void retrieveShouldNotSentAuditEventWhenIntegrationAuditEventsAreDisabled() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);

        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        String executionId = "execId";

        given(integrationContextService.findById(executionId)).willReturn(integrationContextEntity);
        Map<String, Object> variables = Collections.singletonMap("var1",
                "v");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(new IntegrationRequestImpl(integrationContext),
                                                                          new Error("Test Error"));
        //when
        handler.receive(integrationErrorEvent);

        //then
        verify(auditProducer,
                never()).send(any(Message.class));
    }
}
