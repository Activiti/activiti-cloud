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
package org.activiti.cloud.examples.connectors;

import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@ConnectorBinding(
    input = HeadersConnectorChannels.HEADERS_CONNECTOR_CONSUMER,
    condition = "headers['processDefinitionVersion']!=null",
    outputHeader = ""
)
@Component(HeadersConnectorChannels.HEADERS_CONNECTOR_CONSUMER + "Connector")
public class HeadersConnector implements Connector<Message<IntegrationRequest>, Void> {

    private final IntegrationResultSender integrationResultSender;
    private final ConnectorProperties connectorProperties;

    @Autowired
    public HeadersConnector(IntegrationResultSender integrationResultSender, ConnectorProperties connectorProperties) {
        this.integrationResultSender = integrationResultSender;
        this.connectorProperties = connectorProperties;
    }

    @Override
    public Void apply(Message<IntegrationRequest> integrationRequestMessage) {
        MessageHeaders headers = integrationRequestMessage.getHeaders();
        IntegrationRequest integrationRequest = integrationRequestMessage.getPayload();

        Map<String, Object> result = new HashMap<>();

        result.put("processDefinitionVersion", headers.get("processDefinitionVersion"));
        result.put("processDefinitionKey", headers.get("processDefinitionKey"));
        result.put("processDefinitionId", headers.get("processDefinitionId"));

        Message<IntegrationResult> message = IntegrationResultBuilder
            .resultFor(integrationRequest, connectorProperties)
            .withOutboundVariables(result)
            .buildMessage();

        integrationResultSender.send(message);
        return null;
    }
}
