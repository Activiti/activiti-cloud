package org.activiti.services.connectors;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.runtime.api.event.impl.CloudIntegrationRequestedImpl;
import org.activiti.runtime.api.model.IntegrationRequest;
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
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final MessageChannel auditProducer;
    private final BinderAwareChannelResolver resolver;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public IntegrationRequestSender(RuntimeBundleProperties runtimeBundleProperties,
                                    MessageChannel auditProducer,
                                    BinderAwareChannelResolver resolver,
                                    RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.auditProducer = auditProducer;
        this.resolver = resolver;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendIntegrationRequest(IntegrationRequest event) {

        resolver.resolveDestination(event.getIntegrationContext().getConnectorType()).send(buildIntegrationRequestMessage(event));
        sendAuditEvent(event);
    }

    private void sendAuditEvent(IntegrationRequest integrationRequest) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationRequestedImpl integrationRequested = new CloudIntegrationRequestedImpl(integrationRequest.getIntegrationContext());
            runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationRequested);
            auditProducer.send(
                    MessageBuilder.withPayload(integrationRequested).build()
            );
        }
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(IntegrationRequest event) {
        return MessageBuilder.withPayload(event)
                .setHeader(CONNECTOR_TYPE,
                           event.getIntegrationContext().getConnectorType())
                .build();
    }
}
