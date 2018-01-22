package org.activiti.services.connectors;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEventImpl;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

public class IntegrationRequestSender extends TransactionSynchronizationAdapter {

    protected static final String CONNECTOR_TYPE = "connectorType";
    private final MessageChannel integrationEventsProducer;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final MessageChannel auditProducer;
    private final IntegrationContextEntity integrationContextEntity;
    private final DelegateExecution execution;

    public IntegrationRequestSender(MessageChannel integrationEventsProducer,
                                    RuntimeBundleProperties runtimeBundleProperties,
                                    MessageChannel auditProducer,
                                    IntegrationContextEntity integrationContextEntity,
                                    DelegateExecution execution) {
        this.integrationEventsProducer = integrationEventsProducer;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.auditProducer = auditProducer;
        this.integrationContextEntity = integrationContextEntity;
        this.execution = execution;
    }

    @Override
    public void afterCommit() {
        Message<IntegrationRequestEvent> message = buildIntegrationRequestMessage(execution,
                                                                                  integrationContextEntity);

        integrationEventsProducer.send(message);
        sendIntegrationRequestSentEvent(message);
    }

    private void sendIntegrationRequestSentEvent(Message<IntegrationRequestEvent> message) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            IntegrationRequestEvent integrationRequestEvent = message.getPayload();
            IntegrationRequestSentEventImpl event = new IntegrationRequestSentEventImpl(runtimeBundleProperties.getName(),
                                                                                        integrationRequestEvent.getExecutionId(),
                                                                                        integrationRequestEvent.getProcessDefinitionId(),
                                                                                        integrationRequestEvent.getProcessInstanceId(),
                                                                                        integrationRequestEvent.getIntegrationContextId(),
                                                                                        integrationRequestEvent.getFlowNodeId());
            auditProducer.send(
                    MessageBuilder.withPayload(new ProcessEngineEvent[]{event}).build()
            );
        }
    }

    private Message<IntegrationRequestEvent> buildIntegrationRequestMessage(DelegateExecution execution,
                                                                            IntegrationContextEntity integrationContext) {
        IntegrationRequestEvent event = new IntegrationRequestEvent(execution.getProcessInstanceId(),
                                                                    execution.getProcessDefinitionId(),
                                                                    integrationContext.getExecutionId(),
                                                                    integrationContext.getId(),
                                                                    integrationContext.getFlowNodeId(),
                                                                    execution.getVariables());

        String implementation = ((ServiceTask) execution.getCurrentFlowElement()).getImplementation();
        return MessageBuilder.withPayload(event)
                .setHeader(CONNECTOR_TYPE,
                           implementation)
                .build();
    }
}
