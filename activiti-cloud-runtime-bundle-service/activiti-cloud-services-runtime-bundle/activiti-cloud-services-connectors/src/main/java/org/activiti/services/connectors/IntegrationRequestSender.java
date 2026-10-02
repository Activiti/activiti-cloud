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

import java.util.Optional;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.activiti.services.connectors.mtc.MtcIntegrationRequestInterceptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class IntegrationRequestSender {

    public static final String CONNECTOR_TYPE = "connectorType";

    private final StreamBridge streamBridge;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;
    private final FunctionBindingConfiguration.BindingResolver bindingResolver;
    private final Optional<MtcIntegrationRequestInterceptor> mtcInterceptor;

    public IntegrationRequestSender(
        StreamBridge streamBridge,
        IntegrationContextMessageBuilderFactory messageBuilderFactory,
        FunctionBindingConfiguration.BindingResolver bindingResolver,
        Optional<MtcIntegrationRequestInterceptor> mtcInterceptor
    ) {
        this.streamBridge = streamBridge;
        this.messageBuilderFactory = messageBuilderFactory;
        this.bindingResolver = bindingResolver;
        this.mtcInterceptor = mtcInterceptor;
    }

    public void sendIntegrationRequest(IntegrationRequest event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalTransactionStateException("Transaction synchronization must be active.");
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    String connectorType = event.getIntegrationContext().getConnectorType();

                    boolean mtcEnabled = mtcInterceptor
                        .map(interceptor -> interceptor.isMtcEnabled(connectorType))
                        .orElse(false);

                    String destination;
                    String resolvedConnectorType;
                    String binderName = null;
                    if (mtcEnabled) {
                        MtcIntegrationRequestInterceptor interceptor = mtcInterceptor.get();
                        destination = interceptor.resolveMtcDestination(connectorType);
                        resolvedConnectorType = interceptor.resolveConnectorType(connectorType);
                        binderName = interceptor.getBinderName();
                    } else {
                        destination = connectorType;
                        resolvedConnectorType = null;
                    }

                    Message<IntegrationRequest> message = buildIntegrationRequestMessage(
                        event,
                        destination,
                        resolvedConnectorType
                    );
                    if (binderName != null) {
                        streamBridge.send(destination, binderName, message);
                    } else {
                        streamBridge.send(destination, message);
                    }
                }
            }
        );
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(
        IntegrationRequest event,
        String resolvedDestination,
        String resolvedConnectorType
    ) {
        MessageBuilder<IntegrationRequest> builder = messageBuilderFactory
            .create(event.getIntegrationContext())
            .withPayload(event)
            .setHeader(FUNCTION_DESTINATION, bindingResolver.getBindingDestination(resolvedDestination));

        if (resolvedConnectorType != null) {
            builder.setHeader(CONNECTOR_TYPE, resolvedConnectorType);
        }

        return builder.build();
    }
}
