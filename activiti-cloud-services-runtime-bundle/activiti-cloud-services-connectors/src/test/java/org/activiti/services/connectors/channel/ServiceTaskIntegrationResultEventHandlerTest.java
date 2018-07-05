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

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.runtime.api.connector.IntegrationContextImpl;
import org.activiti.runtime.api.event.impl.CloudIntegrationResultReceivedImpl;
import org.activiti.runtime.api.model.IntegrationResult;
import org.activiti.runtime.api.model.impl.IntegrationResultImpl;
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
    private MessageChannel auditProducer;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Captor
    private ArgumentCaptor<Message<CloudIntegrationResultReceivedImpl>> messageCaptor;

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
    }

    @Test
    public void receiveShouldTriggerTheExecutionAndDeleteTheRelatedIntegrationContext() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        integrationContextEntity.setProcessInstanceId(PROC_INST_ID);
        integrationContextEntity.setProcessDefinitionId(PROC_DEF_ID);

        given(integrationContextService.findById(ENTITY_ID))
                .willReturn(integrationContextEntity);
        given(executionQuery.list()).willReturn(Collections.singletonList(mock(Execution.class)));
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        //when
        handler.receive(new IntegrationResultImpl(integrationContext));

        //then
        verify(integrationContextService).deleteIntegrationContext(integrationContextEntity);
        verify(runtimeService).trigger(EXECUTION_ID,
                                       variables);
    }

    @Test
    public void receiveShouldDoNothingWhenIntegrationContextsIsNull() {
        //given
        given(integrationContextService.findById(ENTITY_ID))
                .willReturn(null);
        given(executionQuery.list()).willReturn(Collections.singletonList(mock(Execution.class)));
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        //when
        handler.receive(new IntegrationResultImpl(integrationContext));

        //then
        verify(integrationContextService, never()).deleteIntegrationContext(any());
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

        IntegrationResult integrationResultEvent = new IntegrationResultImpl(integrationContext);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(auditProducer).send(messageCaptor.capture());
        Message<CloudIntegrationResultReceivedImpl> message = messageCaptor.getValue();
        CloudIntegrationResultReceivedImpl event = message.getPayload();
        assertThat(event.getEntity().getId()).isEqualTo(ENTITY_ID);
        assertThat(event.getEntity().getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(event.getEntity().getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(event);
    }

    private IntegrationContextImpl buildIntegrationContext(Map<String, Object> variables) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(ENTITY_ID);
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);
        integrationContext.addOutBoundVariables(variables);
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


        IntegrationResultImpl integrationResultEvent = new IntegrationResultImpl(integrationContext);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(auditProducer,
               never()).send(any(Message.class));
    }
}