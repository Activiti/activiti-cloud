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

package org.activiti.cloud.connectors.starter.model;

import java.util.Map;

import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationResultBuilder {

    private IntegrationRequest requestEvent;

    private IntegrationResultImpl integrationResult;


    private IntegrationResultBuilder(IntegrationRequest integrationRequest, ConnectorProperties connectorProperties) {
        this.requestEvent = integrationRequest;
        this.integrationResult = new IntegrationResultImpl(integrationRequest, integrationRequest.getIntegrationContext());
        if(connectorProperties != null) {
            this.integrationResult.setAppName(connectorProperties.getAppName());
            this.integrationResult.setAppVersion(connectorProperties.getAppVersion());
            this.integrationResult.setServiceFullName(connectorProperties.getServiceFullName());
            this.integrationResult.setServiceType(connectorProperties.getServiceType());
            this.integrationResult.setServiceVersion(connectorProperties.getServiceVersion());
            this.integrationResult.setServiceName(connectorProperties.getServiceName());
        }

    }

    public static IntegrationResultBuilder resultFor(IntegrationRequest integrationRequest, ConnectorProperties connectorProperties) {
        return new IntegrationResultBuilder(integrationRequest, connectorProperties);
    }

    public IntegrationResultBuilder withOutboundVariables(Map<String, Object> variables) {
        integrationResult.getIntegrationContext().addOutBoundVariables(variables);
        return this;
    }

    public IntegrationResult build() {
        return integrationResult;
    }

    public Message<IntegrationResult> buildMessage() {
        return getMessageBuilder().build();
    }

    public MessageBuilder<IntegrationResult> getMessageBuilder() {
        return MessageBuilder.withPayload((IntegrationResult)integrationResult).setHeader("targetService",
                                                                       requestEvent.getServiceFullName());
    }
}
