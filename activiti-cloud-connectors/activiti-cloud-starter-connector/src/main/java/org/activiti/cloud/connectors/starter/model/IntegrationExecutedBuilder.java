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

import java.util.stream.Stream;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.events.CloudIntegrationExecutedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationExecutedEventImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationExecutedBuilder {

    private IntegrationResult integrationResult;
    private final ConnectorProperties properties;


    private IntegrationExecutedBuilder(IntegrationResult integrationResult,ConnectorProperties properties) {
        this.integrationResult = integrationResult;
        this.properties = properties;
    }

    public static IntegrationExecutedBuilder executionFor(IntegrationResult integrationResult,ConnectorProperties properties) {
        return new IntegrationExecutedBuilder(integrationResult, properties);
    }

    public IntegrationResult build() {
        return integrationResult;
    }

    public Message<CloudRuntimeEvent<?, ?>[]> buildMessage() {
        return getMessageBuilder().build();
    }

    public MessageBuilder<CloudRuntimeEvent<?, ?>[]> getMessageBuilder() {
        IntegrationContext integrationContext = integrationResult.getIntegrationContext();

        CloudIntegrationExecutedEvent integrationExecutedEvent = new CloudIntegrationExecutedEventImpl(integrationContext);

        CloudRuntimeEvent<?, ?>[] payload = Stream.of(integrationExecutedEvent)
            .toArray(CloudRuntimeEvent[]::new);

        return MessageBuilder.withPayload(payload)
            .setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
            .setHeader(IntegrationMessageHeaders.CONNECTOR_TYPE, integrationContext.getConnectorType())
            .setHeader(IntegrationMessageHeaders.BUSINESS_KEY, integrationContext.getBusinessKey())
            .setHeader(IntegrationMessageHeaders.INTEGRATION_CONTEXT_ID, integrationContext.getId())
            .setHeader(IntegrationMessageHeaders.PROCESS_INSTANCE_ID, integrationContext.getProcessInstanceId())
            .setHeader(IntegrationMessageHeaders.EXECUTION_ID, integrationContext.getExecutionId())
            .setHeader(IntegrationMessageHeaders.PROCESS_DEFINITION_ID, integrationContext.getProcessDefinitionId())
            .setHeader(IntegrationMessageHeaders.PROCESS_DEFINITION_KEY, integrationContext.getProcessDefinitionKey())
            .setHeader(IntegrationMessageHeaders.PROCESS_DEFINITION_VERSION, integrationContext.getProcessDefinitionVersion())
            .setHeader(IntegrationMessageHeaders.PARENT_PROCESS_INSTANCE_ID, integrationContext.getParentProcessInstanceId())
            .setHeader(IntegrationMessageHeaders.APP_VERSION, integrationContext.getAppVersion())
            .setHeader(IntegrationMessageHeaders.APP_NAME, properties.getAppName())
            .setHeader(IntegrationMessageHeaders.SERVICE_NAME, properties.getServiceName())
            .setHeader(IntegrationMessageHeaders.SERVICE_FULL_NAME, properties.getServiceFullName())
            .setHeader(IntegrationMessageHeaders.SERVICE_TYPE, properties.getServiceType())
            .setHeader(IntegrationMessageHeaders.SERVICE_VERSION, properties.getServiceVersion())
            ;
    }
}
