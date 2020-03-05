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

import java.util.stream.Stream;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class IntegrationRequestSender {
    public static final String CONNECTOR_TYPE = "connectorType";

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final MessageChannel auditProducer;
    private final BinderAwareChannelResolver resolver;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;

    public IntegrationRequestSender(RuntimeBundleProperties runtimeBundleProperties,
                                    MessageChannel auditProducer,
                                    BinderAwareChannelResolver resolver,
                                    RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                    IntegrationContextMessageBuilderFactory messageBuilderFactory) {
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.auditProducer = auditProducer;
        this.resolver = resolver;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.messageBuilderFactory = messageBuilderFactory;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendIntegrationRequest(IntegrationRequest event) {

        resolver.resolveDestination(event.getIntegrationContext().getConnectorType()).send(buildIntegrationRequestMessage(event));
        sendAuditEvent(event);
    }

    @SuppressWarnings("rawtypes")
    private void sendAuditEvent(IntegrationRequest integrationRequest) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationRequestedEventImpl integrationRequested = new CloudIntegrationRequestedEventImpl(integrationRequest.getIntegrationContext());
            runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationRequested);

            Message<CloudRuntimeEvent[]> message = messageBuilderFactory.create(integrationRequest.getIntegrationContext()).withPayload(Stream.of(integrationRequested)
                                                                                        .toArray(CloudRuntimeEvent[]::new))
                .build();

            auditProducer.send(message);
        }
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(IntegrationRequest event) {
        return messageBuilderFactory.create(event.getIntegrationContext())
                .withPayload(event)
                .build();
    }
}
