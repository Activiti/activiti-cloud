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
package org.activiti.services.connectors.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.cloud.services.events.message.MessageBuilderAppenderChain;
import org.activiti.engine.ActivitiEngineAgenda;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntityImpl;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.messaging.MessageChannel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServiceTaskIntegrationResultEventHandlerTest {

    private static final String EXECUTION_ID = "execId";
    private static final String ENTITY_ID = "entityId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String CLIENT_ID = "entityId";
    private static final String CLIENT_NAME = "serviceTaskName";
    private static final String CLIENT_TYPE = ServiceTask.class.getSimpleName();

    @InjectMocks
    private ServiceTaskIntegrationResultEventHandler handler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeService runtimeService;

    @Mock
    private IntegrationContextService integrationContextService;

    @Mock
    private MessageChannel auditProducer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private ManagementService managementService;

    @Mock
    private IntegrationContextMessageBuilderFactory messageBuilderFactory;

    @Captor
    private ArgumentCaptor<CloudRuntimeEvent<?, ?>> messageCaptor;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CommandContext commandContext;

    @Mock
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Mock
    private ExecutionEntity executionEntity;

    @Mock
    private ActivitiEngineAgenda agenda;

    private static MockedStatic<Context> context;

    @BeforeAll
    public static void init() {
        context = Mockito.mockStatic(Context.class);
    }

    @AfterAll
    public static void close() {
        context.close();
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).thenReturn(true);
        when(runtimeBundleProperties.getServiceFullName()).thenReturn("myApp");
        when(runtimeService.createExecutionQuery()
                           .executionId(anyString())
                           .list()).thenReturn(Collections.emptyList());
        when(messageBuilderFactory.create(any())).thenReturn(new MessageBuilderAppenderChain());

        ProcessEngineConfigurationImpl processEngineConfiguration = mock(ProcessEngineConfigurationImpl.class);
        when(processEngineConfiguration.getEventDispatcher()).thenReturn(mock(ActivitiEventDispatcher.class));

        context.when(Context::getProcessEngineConfiguration).thenReturn(processEngineConfiguration);
        context.when(Context::getAgenda).thenReturn(agenda);

        given(executionEntity.getId()).willReturn(EXECUTION_ID);

        given(commandContext.getExecutionEntityManager().findById(anyString())).willReturn(executionEntity);

        given(managementService.executeCommand(any())).willAnswer(invocation -> {
            Command<Void> command = invocation.getArgument(0);

            command.execute(commandContext);

            return null;
        });

    }

    @Test
    public void receive_should_setVariablesTriggerExecutionAndDeleteRelatedIntegrationContext() {
        //given
        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        integrationContextEntity.setProcessInstanceId(PROC_INST_ID);
        integrationContextEntity.setProcessDefinitionId(PROC_DEF_ID);

        given(integrationContextService.findById(ENTITY_ID))
            .willReturn(integrationContextEntity);

        List<Execution> executions = Collections.singletonList(executionEntity);

        when(runtimeService.createExecutionQuery()
                            .executionId(anyString())
                            .list()).thenReturn(executions);
        when(executions.get(0)
                       .getActivityId()).thenReturn(CLIENT_ID);

        Map<String, Object> variables = Collections.singletonMap("var1",
                "v");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        //when
        handler.receive(new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext));

        //then
        verify(integrationContextService).deleteIntegrationContext(integrationContextEntity);
        verify(executionEntity).setVariableLocal("var1", variables.get("var1"), false);
        verify(agenda).planTriggerExecutionOperation(executionEntity);
        verify(processEngineEventsAggregator).add(any(CloudRuntimeEvent.class));
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
        handler.receive(new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext));

        //then
        verify(integrationContextService, never()).deleteIntegrationContext(any());
    }

    @Test
    public void receiveShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);

        IntegrationContextEntityImpl integrationContextEntity = new IntegrationContextEntityImpl();
        integrationContextEntity.setExecutionId(EXECUTION_ID);
        integrationContextEntity.setId(ENTITY_ID);
        integrationContextEntity.setProcessInstanceId(PROC_INST_ID);
        integrationContextEntity.setProcessDefinitionId(PROC_DEF_ID);

        List<Execution> executions = Collections.singletonList(executionEntity);

        when(runtimeService.createExecutionQuery()
                           .executionId(anyString())
                           .list()).thenReturn(executions);
        when(executions.get(0)
                       .getActivityId()).thenReturn(CLIENT_ID);

        given(integrationContextService.findById(ENTITY_ID)).willReturn(integrationContextEntity);
        Map<String, Object> variables = Collections.singletonMap("var1",
                "v");

        given(runtimeBundleProperties.getServiceFullName()).willReturn("myApp");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);

        IntegrationResult integrationResultEvent = new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(processEngineEventsAggregator).add(messageCaptor.capture());
        CloudIntegrationResultReceivedEventImpl event = (CloudIntegrationResultReceivedEventImpl) messageCaptor.getValue();
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
        integrationContext.setExecutionId(EXECUTION_ID);
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
        String executionId = EXECUTION_ID;

        List<Execution> executions = Collections.singletonList(executionEntity);

        when(runtimeService.createExecutionQuery()
                           .executionId(anyString())
                           .list()).thenReturn(executions);
        when(executions.get(0)
                       .getActivityId()).thenReturn(CLIENT_ID);

        given(integrationContextService.findById(ENTITY_ID)).willReturn(integrationContextEntity);
        Map<String, Object> variables = Collections.singletonMap("var1",
                                                                 "v");

        IntegrationContextImpl integrationContext = buildIntegrationContext(variables);
        IntegrationResultImpl integrationResultEvent = new IntegrationResultImpl(new IntegrationRequestImpl(), integrationContext);

        //when
        handler.receive(integrationResultEvent);

        //then
        verify(processEngineEventsAggregator,
               never()).add(any(CloudRuntimeEvent.class));
    }
}
