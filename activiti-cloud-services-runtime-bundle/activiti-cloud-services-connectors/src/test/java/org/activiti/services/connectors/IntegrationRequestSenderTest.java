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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
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
    private MessageChannel integrationProducer;

    @Mock
    private MessageChannel auditProducer;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Mock
    private RuntimeBundleProperties.RuntimeBundleEventsProperties eventsProperties;

    @Mock
    private IntegrationContextEntity integrationContextEntity;

    @Mock
    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<Message<ProcessEngineEvent[]>> auditMessageArgumentCaptor;

    @Captor
    private ArgumentCaptor<Message<IntegrationRequestEvent>> integrationRequestMessageCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(runtimeBundleProperties.getEventsProperties()).thenReturn(eventsProperties);
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);
        when(delegateExecution.getCurrentFlowElement()).thenReturn(serviceTask);
        integrationRequestSender = new IntegrationRequestSender(integrationProducer,
                                                                runtimeBundleProperties,
                                                                auditProducer,
                                                                integrationContextEntity,
                                                                delegateExecution);

        when(delegateExecution.getProcessDefinitionId()).thenReturn(PROC_DEF_ID);
        when(delegateExecution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(integrationContextEntity.getExecutionId()).thenReturn(EXECUTION_ID);
        when(integrationContextEntity.getId()).thenReturn(INTEGRATION_CONTEXT_ID);
        when(integrationContextEntity.getFlowNodeId()).thenReturn(FLOW_NODE_ID);
    }

    @Test
    public void afterCommitShouldSendIntegrationRequestMessage() throws Exception {
        //when
        integrationRequestSender.afterCommit();

        //then
        verify(integrationProducer).send(integrationRequestMessageCaptor.capture());
        Message<IntegrationRequestEvent> integrationRequestEventMessage = integrationRequestMessageCaptor.getValue();

        IntegrationRequestEvent integrationRequestEvent = integrationRequestEventMessage.getPayload();
        assertThat(integrationRequestEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(integrationRequestEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequestEvent.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(integrationRequestEvent.getIntegrationContextId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequestEvent.getFlowNodeId()).isEqualTo(FLOW_NODE_ID);

        assertThat(integrationRequestEventMessage.getHeaders().get(IntegrationRequestSender.CONNECTOR_TYPE)).isEqualTo(CONNECTOR_TYPE);

    }

    @Test
    public void afterCommitShouldNotSendIntegrationAuditEventWhenIntegrationAuditEventsAreDisabled() throws Exception {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(false);
        given(runtimeBundleProperties.getName()).willReturn(APP_NAME);

        //when
        integrationRequestSender.afterCommit();

        //then
        verify(auditProducer,
               never()).send(ArgumentMatchers.any());
    }

    @Test
    public void afterCommitShouldSendIntegrationAuditEventWhenIntegrationAuditEventsAreEnabled() throws Exception {
        //given
        given(runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()).willReturn(true);
        given(runtimeBundleProperties.getName()).willReturn(APP_NAME);

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setImplementation(CONNECTOR_TYPE);

        //when
        integrationRequestSender.afterCommit();

        //then
        verify(auditProducer).send(auditMessageArgumentCaptor.capture());

        Message<ProcessEngineEvent[]> message = auditMessageArgumentCaptor.getValue();
        assertThat(message.getPayload()).hasSize(1);
        assertThat(message.getPayload()[0]).isInstanceOf(IntegrationRequestSentEvent.class);

        IntegrationRequestSentEvent integrationRequestSentEvent = (IntegrationRequestSentEvent) message.getPayload()[0];

        assertThat(integrationRequestSentEvent.getIntegrationContextId()).isEqualTo(INTEGRATION_CONTEXT_ID);
        assertThat(integrationRequestSentEvent.getProcessInstanceId()).isEqualTo(PROC_INST_ID);
        assertThat(integrationRequestSentEvent.getProcessDefinitionId()).isEqualTo(PROC_DEF_ID);
        assertThat(integrationRequestSentEvent.getApplicationName()).isEqualTo(APP_NAME);
    }

}
