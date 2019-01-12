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
package org.activiti.services.connectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.activiti.runtime.api.connector.VariablesMatchHelper;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.activiti.services.test.DelegateExecutionBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class IntegrationRequestSenderTest {

    private static final String MY_PARENT_PROC_ID = "my-parent-proc-id";
    private static final String MY_PROC_DEF_KEY = "my-proc-def-key";
    private static final String PAYMENT_CONNECTOR_TYPE = "payment";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String SPRING_APP_NAME = "springAppName";
    private static final String APP_VERSION = "appVersion";
    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String BUSINESS_KEY = "my-business-key";
    private static final String INTEGRATION_CONTEXT_ID = "intContextId";
    private static final String FLOW_NODE_ID = "myServiceTask";
    private static final String APP_NAME = "appName";

    private IntegrationRequestSender integrationRequestSender;

    @Mock
    private BinderAwareChannelResolver resolver;

    @Mock
    private MessageChannel integrationProducer;

    @Mock
    private MessageChannel auditProducer;

    @Spy
    private RuntimeBundleProperties runtimeBundleProperties = new RuntimeBundleProperties() {
        {
            setAppName(APP_NAME);
            setAppVersion(APP_VERSION);
            setServiceType(SERVICE_TYPE);
            setServiceVersion(SERVICE_VERSION);
            setRbSpringAppName(SPRING_APP_NAME);
        }
    };

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    private IntegrationContextMessageBuilderFactory messageBuilderFactory;

    @Mock
    private IntegrationContextEntity integrationContextEntity;

    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<Message<CloudRuntimeEvent<?,?>>> auditMessageArgumentCaptor;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequest>> integrationRequestMessageCaptor;

    private IntegrationRequestImpl integrationRequest;

    @Before
    public void setUp() {
        initMocks(this);

        messageBuilderFactory = new IntegrationContextMessageBuilderFactory(runtimeBundleProperties);

        integrationRequestSender = new IntegrationRequestSender(runtimeBundleProperties,
                                                                auditProducer,
                                                                resolver,
                                                                runtimeBundleInfoAppender,
                                                                messageBuilderFactory);

        when(resolver.resolveDestination(CONNECTOR_TYPE)).thenReturn(integrationProducer);

        configureProperties();
        configureExecution();
        configureIntegrationContext();

        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);

        IntegrationContextEntity contextEntity = mock(IntegrationContextEntity.class);
        given(contextEntity.getId()).willReturn(INTEGRATION_CONTEXT_ID);

        IntegrationContext integrationContext = new IntegrationContextBuilder(new VariablesMatchHelper()).from(contextEntity, delegateExecution, null);
        integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName(APP_NAME);
    }

    private void configureIntegrationContext() {
        when(integrationContextEntity.getExecutionId()).thenReturn(EXECUTION_ID);
        when(integrationContextEntity.getId()).thenReturn(INTEGRATION_CONTEXT_ID);
        when(integrationContextEntity.getFlowNodeId()).thenReturn(FLOW_NODE_ID);
    }

    private void configureExecution() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        delegateExecution = DelegateExecutionBuilder.anExecution()
                                                    .withServiceTask(serviceTask)
                                                    .withProcessDefinitionId(PROC_DEF_ID)
                                                    .withProcessInstanceId(PROC_INST_ID)
                                                    .withBusinessKey(BUSINESS_KEY)
                                                    .withProcessDefinitionKey(MY_PROC_DEF_KEY)
                                                    .withProcessDefinitionVersion(1)
                                                    .withParentProcessInstanceId(MY_PARENT_PROC_ID)
                                                    .build();
    }

    private void configureProperties() {
        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);
        when(runtimeBundleProperties.getEventsProperties()).thenReturn(eventsProperties);
    }

    @Test
    public void shouldSendIntegrationRequestMessage() {
        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        //then
        verify(integrationProducer).send(integrationRequestMessageCaptor.capture());
        Message<IntegrationRequest> integrationRequestMessage = integrationRequestMessageCaptor.getValue();

        IntegrationRequest sentIntegrationRequestEvent = integrationRequestMessage.getPayload();
        assertThat(sentIntegrationRequestEvent).isEqualTo(integrationRequest);
        assertThat(integrationRequestMessage.getHeaders().get(IntegrationRequestSender.CONNECTOR_TYPE)).isEqualTo(CONNECTOR_TYPE);
    }

    @Test
    public void shouldNotSendIntegrationAuditEventWhenIntegrationAuditEventsAreDisabled() {
        //given
        given(eventsProperties.isIntegrationAuditEventsEnabled()).willReturn(false);

        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        //then
        verify(auditProducer,
               never()).send(ArgumentMatchers.any());
    }

    @Test
    public void shouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() {
        //given
        given(eventsProperties.isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequest);

        //then
        verify(auditProducer).send(auditMessageArgumentCaptor.capture());

        Message<CloudRuntimeEvent<?, ?>> message = auditMessageArgumentCaptor.getValue();
        assertThat(message.getPayload()).isInstanceOf(CloudIntegrationRequestedImpl.class);

        Assertions.assertThat(message.getHeaders())
            .containsKey("routingKey")
            .containsKey("messagePayloadType")
            .containsEntry("parentProcessInstanceId",MY_PARENT_PROC_ID)
            .containsEntry("processDefinitionKey", MY_PROC_DEF_KEY)
            .containsEntry("processDefinitionVersion", 1)
            .containsEntry("businessKey", BUSINESS_KEY)
            .containsEntry("connectorType", PAYMENT_CONNECTOR_TYPE)
            .containsEntry("integrationContextId", INTEGRATION_CONTEXT_ID)
            .containsEntry("processInstanceId", PROC_INST_ID)
            .containsEntry("processDefinitionId", PROC_DEF_ID)
            .containsEntry("appName", APP_NAME)
            .containsEntry("appVersion", APP_VERSION)
            .containsEntry("serviceName",SPRING_APP_NAME)
            .containsEntry("serviceType",SERVICE_TYPE)
            .containsEntry("serviceVersion",SERVICE_VERSION)
            .containsEntry("serviceFullName",APP_NAME);


        CloudIntegrationRequestedImpl integrationRequested = (CloudIntegrationRequestedImpl) message.getPayload();

        assertThat(integrationRequested.getEntity().getId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequested.getEntity().getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequested.getEntity().getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(integrationRequested);
    }

}