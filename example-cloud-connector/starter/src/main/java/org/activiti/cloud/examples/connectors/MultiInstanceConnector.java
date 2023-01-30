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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.common.messaging.functional.Connector;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.activiti.cloud.examples.connectors.MultiInstanceConnector.Channels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

@ConnectorBinding(input = Channels.CHANNEL, condition = "", outputHeader = "")
@Component(Channels.CHANNEL + "Connector")
public class MultiInstanceConnector implements Connector<IntegrationRequest, Void> {

    private final IntegrationResultSender integrationResultSender;
    private final ConnectorProperties connectorProperties;
    private final AtomicInteger counter = new AtomicInteger(0);

    public interface Channels {
        String CHANNEL = "miCloudConnectorInput";

        @InputBinding(CHANNEL)
        default SubscribableChannel miCloudConnectorInput() {
            return MessageChannels.publishSubscribe(CHANNEL).get();
        }
    }

    @Autowired
    public MultiInstanceConnector(
        IntegrationResultSender integrationResultSender,
        ConnectorProperties connectorProperties
    ) {
        this.integrationResultSender = integrationResultSender;
        this.connectorProperties = connectorProperties;
    }

    @Override
    public Void apply(IntegrationRequest integrationRequest) {
        Integer instanceCount = getVariableValue(integrationRequest.getIntegrationContext(), "instanceCount");
        if (instanceCount == counter.get()) {
            counter.set(0);
        }

        Map<String, Object> result = Collections.singletonMap("executionCount", counter.incrementAndGet());

        Message<IntegrationResult> message = IntegrationResultBuilder
            .resultFor(integrationRequest, connectorProperties)
            .withOutboundVariables(result)
            .buildMessage();

        integrationResultSender.send(message);
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getVariableValue(IntegrationContext context, String name) {
        return (T) context.getInBoundVariables().get(name);
    }
}
