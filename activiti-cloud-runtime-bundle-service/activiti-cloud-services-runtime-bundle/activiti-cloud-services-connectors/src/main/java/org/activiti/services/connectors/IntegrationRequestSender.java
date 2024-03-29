/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class IntegrationRequestSender {

    public static final String CONNECTOR_TYPE = "connectorType";

    private final StreamBridge streamBridge;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;

    public IntegrationRequestSender(
        StreamBridge streamBridge,
        IntegrationContextMessageBuilderFactory messageBuilderFactory
    ) {
        this.streamBridge = streamBridge;
        this.messageBuilderFactory = messageBuilderFactory;
    }

    public void sendIntegrationRequest(IntegrationRequest event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalTransactionStateException("Transaction synchronization must be active.");
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    streamBridge.send(
                        event.getIntegrationContext().getConnectorType(),
                        buildIntegrationRequestMessage(event)
                    );
                }
            }
        );
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(IntegrationRequest event) {
        return messageBuilderFactory.create(event.getIntegrationContext()).withPayload(event).build();
    }
}
