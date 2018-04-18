package org.activiti.services.connectors;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.builders.ApplicationBuilderService;
import org.activiti.cloud.services.events.builders.ServiceBuilderService;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.integration.IntegrationRequestSentEventImpl;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IntegrationRequestSender {

    protected static final String CONNECTOR_TYPE = "connectorType";
    private final ApplicationBuilderService applicationBuilderService;
    private final ServiceBuilderService serviceBuilderService;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final MessageChannel auditProducer;
    private final BinderAwareChannelResolver resolver;

    public IntegrationRequestSender(ApplicationBuilderService applicationBuilderService,
                                    ServiceBuilderService serviceBuilderService,
                                    RuntimeBundleProperties runtimeBundleProperties,
                                    MessageChannel auditProducer,
                                    BinderAwareChannelResolver resolver) {
        this.applicationBuilderService = applicationBuilderService;
        this.serviceBuilderService = serviceBuilderService;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.auditProducer = auditProducer;
        this.resolver = resolver;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendIntegrationRequest(IntegrationRequestEvent event) {

        resolver.resolveDestination(event.getConnectorType()).send(buildIntegrationRequestMessage(event));
        sendAuditEvent(event);
    }

    private void sendAuditEvent(IntegrationRequestEvent integrationRequestEvent) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            IntegrationRequestSentEventImpl event = new IntegrationRequestSentEventImpl(serviceBuilderService.buildService(),
                                                                                        applicationBuilderService.buildApplication(),
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

    private Message<IntegrationRequestEvent> buildIntegrationRequestMessage(IntegrationRequestEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(CONNECTOR_TYPE,
                           event.getConnectorType())
                .build();
    }
}
