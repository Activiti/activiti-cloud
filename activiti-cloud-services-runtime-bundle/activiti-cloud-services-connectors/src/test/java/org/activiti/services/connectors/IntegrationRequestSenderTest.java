package org.activiti.services.connectors;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEvent;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
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
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Mock
    private IntegrationContextEntity integrationContextEntity;

    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<Message<ProcessEngineEvent[]>> auditMessageArgumentCaptor;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequestEvent>> integrationRequestMessageCaptor;

    private IntegrationRequestEvent integrationRequestEvent;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        integrationRequestSender = new IntegrationRequestSender(runtimeBundleProperties,
                                                                auditProducer,
                                                                resolver);

        when(resolver.resolveDestination(CONNECTOR_TYPE)).thenReturn(integrationProducer);

        configureProperties();
        configureExecution();
        configureIntegrationContext();

        when(runtimeBundleProperties.getServiceFullName()).thenReturn(APP_NAME);

        integrationRequestEvent = new IntegrationRequestEvent(delegateExecution,
                                                              integrationContextEntity,
                                                              runtimeBundleProperties.getAppName(),
                runtimeBundleProperties.getAppVersion(),
                runtimeBundleProperties.getServiceName(),
                runtimeBundleProperties.getServiceFullName(),
                runtimeBundleProperties.getServiceType(),
                runtimeBundleProperties.getServiceVersion());
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
    public void shouldSendIntegrationRequestMessage() throws Exception {
        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequestEvent);

        //then
        verify(integrationProducer).send(integrationRequestMessageCaptor.capture());
        Message<IntegrationRequestEvent> integrationRequestEventMessage = integrationRequestMessageCaptor.getValue();

        IntegrationRequestEvent sentIntegrationRequestEvent = integrationRequestEventMessage.getPayload();
        assertThat(sentIntegrationRequestEvent).isEqualTo(integrationRequestEvent);
        assertThat(integrationRequestEventMessage.getHeaders().get(IntegrationRequestSender.CONNECTOR_TYPE)).isEqualTo(CONNECTOR_TYPE);
    }

    @Test
    public void shouldNotSendIntegrationAuditEventWhenIntegrationAuditEventsAreDisabled() throws Exception {
        //given
        given(eventsProperties.isIntegrationAuditEventsEnabled()).willReturn(false);

        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequestEvent);

        //then
        verify(auditProducer,
               never()).send(ArgumentMatchers.any());
    }

    @Test
    public void shouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() throws Exception {
        //given
        given(eventsProperties.isIntegrationAuditEventsEnabled()).willReturn(true);

        //when
        integrationRequestSender.sendIntegrationRequest(integrationRequestEvent);

        //then
        verify(auditProducer).send(auditMessageArgumentCaptor.capture());

        Message<ProcessEngineEvent[]> message = auditMessageArgumentCaptor.getValue();
        assertThat(message.getPayload()).hasSize(1);
        assertThat(message.getPayload()[0]).isInstanceOf(IntegrationRequestSentEvent.class);

        IntegrationRequestSentEvent integrationRequestSentEvent = (IntegrationRequestSentEvent) message.getPayload()[0];

        assertThat(integrationRequestSentEvent.getIntegrationContextId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequestSentEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequestSentEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(integrationRequestSentEvent.getServiceFullName()).isEqualTo(APP_NAME);
    }
}