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
package org.activiti.cloud.connectors.starter.model;

import java.util.Objects;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationWarning;
import org.activiti.cloud.api.process.model.impl.IntegrationWarningImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationWarningBuilder {

    private final IntegrationRequest integrationRequest;
    private final ConnectorProperties connectorProperties;
    private final String warningCode;
    private final String warningMessage;

    private IntegrationWarningBuilder(
        IntegrationRequest integrationRequest,
        ConnectorProperties connectorProperties,
        String warningCode,
        String warningMessage
    ) {
        this.integrationRequest = integrationRequest;
        this.connectorProperties = connectorProperties;
        this.warningCode = warningCode;
        this.warningMessage = warningMessage;
    }

    public static IntegrationWarningBuilder warningFor(
        IntegrationRequest integrationRequest,
        ConnectorProperties connectorProperties,
        String warningCode,
        String warningMessage
    ) {
        return new IntegrationWarningBuilder(integrationRequest, connectorProperties, warningCode, warningMessage);
    }

    public IntegrationWarning build() {
        Objects.requireNonNull(integrationRequest);
        Objects.requireNonNull(warningCode);

        IntegrationWarningImpl integrationWarning = new IntegrationWarningImpl(
            integrationRequest,
            warningCode,
            warningMessage
        );
        if (connectorProperties != null) {
            integrationWarning.setAppVersion(connectorProperties.getAppVersion());
            integrationWarning.setServiceFullName(connectorProperties.getServiceFullName());
            integrationWarning.setServiceType(connectorProperties.getServiceType());
            integrationWarning.setServiceVersion(connectorProperties.getServiceVersion());
            integrationWarning.setServiceName(connectorProperties.getServiceName());
        }

        return integrationWarning;
    }

    public Message<IntegrationWarning> buildMessage() {
        return getMessageBuilder().build();
    }

    public MessageBuilder<IntegrationWarning> getMessageBuilder() {
        IntegrationWarning integrationWarning = build();

        return MessageBuilder
            .withPayload(integrationWarning)
            .setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
            .setHeader("targetAppName", integrationRequest.getAppName())
            .setHeader("targetService", integrationRequest.getServiceFullName());
    }
}
