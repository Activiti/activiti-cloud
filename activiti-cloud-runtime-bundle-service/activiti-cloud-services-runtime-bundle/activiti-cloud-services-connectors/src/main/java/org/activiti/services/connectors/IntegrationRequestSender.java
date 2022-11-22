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
import org.activiti.cloud.common.messaging.config.FunctionBindingConfiguration.ChannelResolver;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class IntegrationRequestSender {
    public static final String CONNECTOR_TYPE = "connectorType";

    private final ChannelResolver resolver;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;

    public IntegrationRequestSender(ChannelResolver resolver,
                                    IntegrationContextMessageBuilderFactory messageBuilderFactory) {
        this.resolver = resolver;
        this.messageBuilderFactory = messageBuilderFactory;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendIntegrationRequest(IntegrationRequest event) {
        resolver.resolveDestination(event.getIntegrationContext()
                                         .getConnectorType()).send(buildIntegrationRequestMessage(event));
    }

    private Message<IntegrationRequest> buildIntegrationRequestMessage(IntegrationRequest event) {
        return messageBuilderFactory.create(event.getIntegrationContext())
                .withPayload(event)
                .build();
    }
}
