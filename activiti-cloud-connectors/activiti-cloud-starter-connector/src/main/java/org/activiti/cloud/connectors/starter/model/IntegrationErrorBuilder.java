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
package org.activiti.cloud.connectors.starter.model;

import java.util.Objects;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

public class IntegrationErrorBuilder {
    private final IntegrationRequest integrationRequest;
    private final ConnectorProperties connectorProperties;
    private final Throwable error;
    private String destination;

    private IntegrationErrorBuilder(IntegrationRequest integrationRequest,
                                    ConnectorProperties connectorProperties,
                                    Throwable error) {
        this.integrationRequest = integrationRequest;
        this.connectorProperties = connectorProperties;
        this.error = error;

    }

    public static IntegrationErrorBuilder errorFor(IntegrationRequest integrationRequest,
                                                   ConnectorProperties connectorProperties,
                                                   Throwable error) {
        return new IntegrationErrorBuilder(integrationRequest,
                                           connectorProperties,
                                           error);
    }

    public IntegrationErrorBuilder withDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public IntegrationError build() {
        Objects.requireNonNull(integrationRequest);
        Objects.requireNonNull(error);

        IntegrationErrorImpl integrationError = new IntegrationErrorImpl(integrationRequest,
                                                                         error);
        if (connectorProperties != null) {
            integrationError.setAppVersion(connectorProperties.getAppVersion());
            integrationError.setServiceFullName(connectorProperties.getServiceFullName());
            integrationError.setServiceType(connectorProperties.getServiceType());
            integrationError.setServiceVersion(connectorProperties.getServiceVersion());
            integrationError.setServiceName(connectorProperties.getServiceName());
        }

        return integrationError;
    }

    public Message<IntegrationError> buildMessage() {
        return getMessageBuilder().build();
    }

    public MessageBuilder<IntegrationError> getMessageBuilder() {
        IntegrationError integrationError = build();

        MessageBuilder builder = MessageBuilder.withPayload(integrationError)
                             .setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
                             .setHeader("targetAppName", integrationRequest.getAppName())
                             .setHeader("targetService", integrationRequest.getServiceFullName());

        if(StringUtils.hasText(destination)){
            builder.setHeader("spring.cloud.stream.sendto.destination", destination);
        }

        return builder;
    }
}
