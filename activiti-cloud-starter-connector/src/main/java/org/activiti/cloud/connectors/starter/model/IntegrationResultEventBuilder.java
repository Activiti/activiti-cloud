/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationResultEventBuilder {

    private final IntegrationRequestEvent requestEvent;

    private IntegrationResultEvent integrationResultEvent;

    private IntegrationResultEventBuilder(IntegrationRequestEvent requestEvent) {
        this.requestEvent = requestEvent;
        this.integrationResultEvent = new IntegrationResultEvent();
    }

    public static IntegrationResultEventBuilder resultFor(IntegrationRequestEvent requestEvent) {
        return new IntegrationResultEventBuilder(requestEvent)
                .withExecutionId(requestEvent.getExecutionId())
                .withFlowNodeId(requestEvent.getFlowNodeId())
                .withTargetApplication(requestEvent.getApplicationName());
    }

    private IntegrationResultEventBuilder withExecutionId(String executionId) {
        integrationResultEvent.setExecutionId(executionId);
        return this;
    }

    private IntegrationResultEventBuilder withFlowNodeId(String flowNodeId) {
        integrationResultEvent.setFlowNodeId(flowNodeId);
        return this;
    }

    private IntegrationResultEventBuilder withTargetApplication(String targetApplication) {
        integrationResultEvent.setTargetApplication(targetApplication);
        return this;
    }

    public IntegrationResultEventBuilder withVariables(Map<String, Object> variables) {
        integrationResultEvent.setVariables(variables);
        return this;
    }

    public IntegrationResultEvent build() {
        return integrationResultEvent;
    }

    public Message<IntegrationResultEvent> buildMessage() {
        return getMessageBuilder().build();
    }

    public MessageBuilder<IntegrationResultEvent> getMessageBuilder() {
        return MessageBuilder.withPayload(integrationResultEvent).setHeader("targetApplication",
                                                                            requestEvent.getApplicationName());
    }
}
