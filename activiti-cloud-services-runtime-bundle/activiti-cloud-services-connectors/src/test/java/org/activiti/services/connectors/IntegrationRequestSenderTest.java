package org.activiti.services.connectors;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudIntegrationRequestedImpl;
import org.activiti.runtime.api.model.IntegrationContext;
import org.activiti.runtime.api.model.IntegrationRequest;
import org.activiti.runtime.api.model.impl.IntegrationRequestImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.activiti.services.test.DelegateExecutionBuilder.anExecution;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrationRequestSenderTest {

    private static final String CONNECTOR_TYPE = "payment";
    private static final String EXECUTION_ID = "execId";
    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String INTEGRATION_CONTEXT_ID = "intContextId";
    private static final String FLOW_NODE_ID = "myServiceTask";
    private static final String APP_NAME = "myApp";

    private IntegrationRequestSender integrationRequestSender;

    @Mock
    private BinderAwareChannelResolver resolver;

    @Mock
    private MessageChannel integrationProducer;

    @Mock
    private MessageChannel auditProducer;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

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

        integrationRequestSender = new IntegrationRequestSender(runtimeBundleProperties,
                                                                auditProducer,
                                                                resolver,
                                                                runtimeBundleInfoAppender);

        when(resolver.resolveDestination(CONNECTOR_TYPE)).thenReturn(integrationProducer);

        configureProperties();
        configureExecution();
        configureIntegrationContext();

        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);

        IntegrationContextEntity contextEntity = mock(IntegrationContextEntity.class);
        given(contextEntity.getId()).willReturn(INTEGRATION_CONTEXT_ID);
        IntegrationContext integrationContext = new IntegrationContextBuilder().from(contextEntity, delegateExecution);
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

        delegateExecution = anExecution()
                .withServiceTask(serviceTask)
                .withProcessDefinitionId(PROC_DEF_ID)
                .withProcessInstanceId(PROC_INST_ID)
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

        CloudIntegrationRequestedImpl integrationRequested = (CloudIntegrationRequestedImpl) message.getPayload();

        assertThat(integrationRequested.getEntity().getId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequested.getEntity().getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequested.getEntity().getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(integrationRequested);
    }
}