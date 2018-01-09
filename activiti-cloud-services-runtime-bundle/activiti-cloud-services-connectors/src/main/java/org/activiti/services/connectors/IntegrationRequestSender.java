package org.activiti.services.connectors;

import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEventImpl;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.services.connectors.channel.ProcessEngineIntegrationChannels;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

public class IntegrationRequestSender extends TransactionSynchronizationAdapter {

    private static final String CONNECTOR_TYPE = "connectorType";
    private final ProcessEngineIntegrationChannels integrationChannels;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ProcessEngineChannels processEngineChannels;
    private final IntegrationContextEntity integrationContextEntity;
    private final DelegateExecution execution;

    public IntegrationRequestSender(ProcessEngineIntegrationChannels processEngineIntegrationChannels,
                                    RuntimeBundleProperties runtimeBundleProperties,
                                    ProcessEngineChannels processEngineChannels,
                                    IntegrationContextEntity integrationContextEntity,
                                    DelegateExecution execution) {
        this.integrationChannels = processEngineIntegrationChannels;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.processEngineChannels = processEngineChannels;
        this.integrationContextEntity = integrationContextEntity;
        this.execution = execution;
    }

    @Override
    public void afterCommit() {

        Message message = buildMessage(execution,
                integrationContextEntity);


        if (message != null) {
            integrationChannels.integrationEventsProducer().send(message);
            sendIntegrationRequestSentEvent(message);
        }
    }

    private void sendIntegrationRequestSentEvent(Message<IntegrationRequestEvent> message) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            IntegrationRequestEvent integrationRequestEvent = message.getPayload();
            IntegrationRequestSentEventImpl event = new IntegrationRequestSentEventImpl(runtimeBundleProperties.getName(),
                    integrationRequestEvent.getExecutionId(),
                    integrationRequestEvent.getProcessDefinitionId(),
                    integrationRequestEvent.getProcessInstanceId(),
                    integrationRequestEvent.getIntegrationContextId());
            processEngineChannels.auditProducer().send(
                    MessageBuilder.withPayload(new ProcessEngineEvent[]{event}).build()
            );
        }
    }

    private Message<IntegrationRequestEvent> buildMessage(DelegateExecution execution,
                                                          IntegrationContextEntity integrationContext) {
        IntegrationRequestEvent event = new IntegrationRequestEvent(execution.getProcessInstanceId(),
                execution.getProcessDefinitionId(),
                integrationContext.getExecutionId(),
                integrationContext.getId(),
                execution.getVariables());

        String implementation = ((ServiceTask) execution.getCurrentFlowElement()).getImplementation();
        return MessageBuilder.withPayload(event)
                .setHeader(CONNECTOR_TYPE,
                        implementation)
                .build();
    }
}
