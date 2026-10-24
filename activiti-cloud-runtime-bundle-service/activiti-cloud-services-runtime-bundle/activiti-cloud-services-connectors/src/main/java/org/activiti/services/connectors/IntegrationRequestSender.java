/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.services.connectors;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class IntegrationRequestSender {

    public static final String CONNECTOR_TYPE = "connectorType";

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationRequestSender.class);

    private final StreamBridge streamBridge;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;
    private final FunctionBindingConfiguration.BindingResolver bindingResolver;
    private final IntegrationRequestReloadService integrationRequestReloadService;

    public IntegrationRequestSender(
        StreamBridge streamBridge,
        IntegrationContextMessageBuilderFactory messageBuilderFactory,
        FunctionBindingConfiguration.BindingResolver bindingResolver,
        IntegrationRequestReloadService integrationRequestReloadService
    ) {
        this.streamBridge = streamBridge;
        this.messageBuilderFactory = messageBuilderFactory;
        this.bindingResolver = bindingResolver;
        this.integrationRequestReloadService = integrationRequestReloadService;
    }

    public void sendIntegrationRequest(IntegrationRequest event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalTransactionStateException("Transaction synchronization must be active.");
        }

        PendingIntegrationDispatch pendingDispatch = new PendingIntegrationDispatch(
            event.getIntegrationContext().getId(),
            event.getIntegrationContext().getConnectorType()
        );

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchIntegrationRequest(pendingDispatch);
                }
            }
        );
    }

    private void dispatchIntegrationRequest(PendingIntegrationDispatch pendingDispatch) {
        try {
            integrationRequestReloadService
                .reload(pendingDispatch.integrationContextId())
                .ifPresent(integrationRequest ->
                    streamBridge.send(
                        pendingDispatch.connectorType(),
                        buildIntegrationRequestMessage(integrationRequest)
                    )
                );
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Unable to dispatch integration request for integration context '{}'",
                pendingDispatch.integrationContextId(),
                exception
            );
        }
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(IntegrationRequest event) {
        var destination = bindingResolver.getBindingDestination(event.getIntegrationContext().getConnectorType());

        return messageBuilderFactory
            .create(event.getIntegrationContext())
            .withPayload(event)
            .setHeader(FUNCTION_DESTINATION, bindingResolver.getBindingDestination(destination))
            .build();
    }

    private record PendingIntegrationDispatch(String integrationContextId, String connectorType) {}
}
