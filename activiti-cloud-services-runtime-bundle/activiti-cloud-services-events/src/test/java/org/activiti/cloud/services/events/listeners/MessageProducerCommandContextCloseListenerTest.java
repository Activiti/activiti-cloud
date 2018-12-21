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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.ExecutionContextMessageBuilderFactory;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class MessageProducerCommandContextCloseListenerTest {

    private static final String MOCK_ROUTING_KEY = "springAppName.appName.mockProcessDefinitionKey.mockProcessInstanceId.mockBusinessKey";
    private static final String MOCK_PARENT_PROCESS_NAME = "mockParentProcessName";
    private static final String LORG_ACTIVITI_CLOUD_API_MODEL_SHARED_EVENTS_CLOUD_RUNTIME_EVENT = "[Lorg.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;";
    private static final String MOCK_PROCESS_NAME = "mockProcessName";
    private static final String SPRING_APP_NAME = "springAppName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String APP_VERSION = "appVersion";
    private static final String APP_NAME = "appName";
    private static final String MOCK_DEPLOYMENT_NAME = "mockDeploymentName";
    private static final String MOCK_DEPLOYMENT_ID = "mockDeploymentId";
    private static final int MOCK_PROCESS_DEFINITION_VERSION = 0;
    private static final String MOCK_PROCESS_DEFINITION_KEY = "mockProcessDefinitionKey";
    private static final String MOCK_PROCESS_DEFINITION_ID = "mockProcessDefinitionId";
    private static final String MOCK_PARENT_PROCESS_INSTANCE_ID = "mockParentId";
    private static final String MOCK_PROCESS_INSTANCE_ID = "mockProcessInstanceId";
    private static final String MOCK_BUSINESS_KEY = "mockBusinessKey";
    private static final String MOCK_SUPER_EXECTUION_ID = "mockSuperExectuionId";
    private static final String MOCK_PROCESS_DEFINITION_NAME = "mockProcessDefinitionName";
    
    @InjectMocks
    private MessageProducerCommandContextCloseListener closeListener;

    @Mock
    private ProcessEngineChannels producer;

    @Spy
    private RuntimeBundleProperties properties = new RuntimeBundleProperties() {
        {
            setAppName(APP_NAME);
            setAppVersion(APP_VERSION);
            setServiceType(SERVICE_TYPE);
            setServiceVersion(SERVICE_VERSION);
            setRbSpringAppName(SPRING_APP_NAME);
        }
    };

    @Spy
    private ExecutionContextMessageBuilderFactory messageBuilderChainFactory = 
                new ExecutionContextMessageBuilderFactory(properties);

    @Spy
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender = 
                new RuntimeBundleInfoAppender(properties);
    
    @Mock
    private MessageChannel auditChannel;

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<Message<CloudRuntimeEvent<?, ?>[]>> messageArgumentCaptor;

    private CloudRuntimeEventImpl<?, ?> event;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        event = new CloudProcessCreatedEventImpl();
        
        when(producer.auditProducer()).thenReturn(auditChannel);
        
        ExecutionContext executionContext = mockExecutionContext();
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.EXECUTION_CONTEXT))
                .willReturn(executionContext);
        
    }

    @Test
    public void closedShouldSendEventsRegisteredOnTheCommandContext() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(Collections.singletonList(event));

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel).send(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue()
                                        .getPayload()).containsExactly(event);
        
        CloudRuntimeEvent<?, ?>[] result = messageArgumentCaptor.getValue().getPayload();
        
        assertThat(result).hasSize(1);

        assertThat(result[0].getProcessInstanceId()).isEqualTo(MOCK_PROCESS_INSTANCE_ID);
        assertThat(result[0].getParentProcessInstanceId()).isEqualTo(MOCK_PARENT_PROCESS_INSTANCE_ID);
        assertThat(result[0].getBusinessKey()).isEqualTo(MOCK_BUSINESS_KEY);
        assertThat(result[0].getProcessDefinitionId()).isEqualTo(MOCK_PROCESS_DEFINITION_ID);
        assertThat(result[0].getProcessDefinitionKey()).isEqualTo(MOCK_PROCESS_DEFINITION_KEY);
        assertThat(result[0].getProcessDefinitionVersion()).isEqualTo(MOCK_PROCESS_DEFINITION_VERSION);

        assertThat(result[0].getAppName()).isEqualTo(APP_NAME);
        assertThat(result[0].getAppVersion()).isEqualTo(APP_VERSION);
        assertThat(result[0].getServiceName()).isEqualTo(SPRING_APP_NAME);
        assertThat(result[0].getServiceType()).isEqualTo(SERVICE_TYPE);
        assertThat(result[0].getServiceVersion()).isEqualTo(SERVICE_VERSION);        
    }

    @Test
    public void closedShouldDoNothingWhenRegisteredEventsIsNull() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(null);

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel,
               never()).send(any());
    }

    @Test
    public void closedShouldDoNothingWhenRegisteredEventsIsEmpty() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(Collections.emptyList());

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel,
               never()).send(any());
    }

    @Test
    public void closedShouldSendMessageHeadersWithExecutionContext() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(Collections.singletonList(event));

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel).send(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue()
                                        .getHeaders()).containsEntry("routingKey", MOCK_ROUTING_KEY)
                                                      .containsEntry("messagePayloadType",LORG_ACTIVITI_CLOUD_API_MODEL_SHARED_EVENTS_CLOUD_RUNTIME_EVENT)
                                                      .containsEntry("businessKey",MOCK_BUSINESS_KEY)
                                                      .containsEntry("processInstanceId",MOCK_PROCESS_INSTANCE_ID)
                                                      .containsEntry("processName",MOCK_PROCESS_NAME)
                                                      .containsEntry("parentProcessInstanceId",MOCK_PARENT_PROCESS_INSTANCE_ID)
                                                      .containsEntry("parentProcessInstanceName",MOCK_PARENT_PROCESS_NAME)
                                                      .containsEntry("processDefinitionId",MOCK_PROCESS_DEFINITION_ID)
                                                      .containsEntry("processDefinitionKey",MOCK_PROCESS_DEFINITION_KEY)
                                                      .containsEntry("processDefinitionVersion", MOCK_PROCESS_DEFINITION_VERSION)
                                                      .containsEntry("deploymentId",MOCK_DEPLOYMENT_ID)
                                                      .containsEntry("deploymentName",MOCK_DEPLOYMENT_NAME)
                                                      .containsEntry("appName", APP_NAME)
                                                      .containsEntry("appVersion", APP_VERSION)
                                                      .containsEntry("serviceName",SPRING_APP_NAME)
                                                      .containsEntry("serviceType", SERVICE_TYPE)
                                                      .containsEntry("serviceVersion", SERVICE_VERSION)
                                                      .containsEntry("serviceFullName",SPRING_APP_NAME);
        
    }

    private ExecutionContext mockExecutionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        ExecutionEntity processInstance = mock(ExecutionEntity.class);
        DeploymentEntity deploymentEntity = mock(DeploymentEntity.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);

        when(context.getProcessInstance()).thenReturn(processInstance);
        when(context.getDeployment()).thenReturn(deploymentEntity);
        when(context.getProcessDefinition()).thenReturn(processDefinition);

        when(processInstance.getId()).thenReturn(MOCK_PROCESS_INSTANCE_ID);
        when(processInstance.getBusinessKey()).thenReturn(MOCK_BUSINESS_KEY);
        when(processInstance.getName()).thenReturn(MOCK_PROCESS_NAME);

        ExecutionEntity superExectuion = mock(ExecutionEntity.class);
        when(processInstance.getSuperExecutionId()).thenReturn(MOCK_SUPER_EXECTUION_ID);
        when(processInstance.getSuperExecution()).thenReturn(superExectuion);

        ExecutionEntity parentProcessInstance = mock(ExecutionEntity.class);
        when(superExectuion.getProcessInstanceId()).thenReturn(MOCK_PARENT_PROCESS_INSTANCE_ID);
        when(superExectuion.getProcessInstance()).thenReturn(parentProcessInstance);
        when(parentProcessInstance.getId()).thenReturn(MOCK_PARENT_PROCESS_INSTANCE_ID);
        when(parentProcessInstance.getName()).thenReturn(MOCK_PARENT_PROCESS_NAME);
        
        when(processDefinition.getId()).thenReturn(MOCK_PROCESS_DEFINITION_ID);
        when(processDefinition.getKey()).thenReturn(MOCK_PROCESS_DEFINITION_KEY);
        when(processDefinition.getVersion()).thenReturn(MOCK_PROCESS_DEFINITION_VERSION);
        when(processDefinition.getName()).thenReturn(MOCK_PROCESS_DEFINITION_NAME);

        when(deploymentEntity.getId()).thenReturn(MOCK_DEPLOYMENT_ID);
        when(deploymentEntity.getName()).thenReturn(MOCK_DEPLOYMENT_NAME);
        
        return context;
    }
}