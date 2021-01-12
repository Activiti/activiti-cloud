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
package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationExecutedBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class IntegrationResultSenderImpl implements IntegrationResultSender {

    private final IntegrationResultChannelResolver resolver;
    private final AuditChannels auditChannels;
    private final ConnectorProperties properties;

    public IntegrationResultSenderImpl(IntegrationResultChannelResolver resolver, AuditChannels auditChannels, ConnectorProperties properties) {
        this.resolver = resolver;
        this.auditChannels = auditChannels;
        this.properties = properties;
    }

    @Override
    public void send(Message<IntegrationResult> message) {
        IntegrationRequest request = message.getPayload().getIntegrationRequest();

        MessageChannel destination = resolver.resolveDestination(request);

        destination.send(message);
        sendAuditMessage(message.getPayload());
    }

    private void sendAuditMessage(IntegrationResult integrationResult) {
        Message<CloudRuntimeEvent<?, ?>[]> message = IntegrationExecutedBuilder.executionFor(integrationResult, properties).buildMessage();
        auditChannels.auditProducer().send(message);
    }
}
