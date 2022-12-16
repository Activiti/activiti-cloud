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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.activiti.cloud.connectors.starter.model.IntegrationResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(TestErrorConnector.Channels.class)
public class TestErrorConnector {

    private static final Logger logger = LoggerFactory.getLogger(TestErrorConnector.class);

    private final IntegrationResultSender integrationResultSender;
    private final ConnectorProperties connectorProperties;

    private CountDownLatch countDownLatch;

    public interface Channels {
        String CHANNEL = "testErrorConnectorInput";

        @Input(CHANNEL)
        SubscribableChannel testErrorConnectorInput();
    }

    public TestErrorConnector(
        IntegrationResultSender integrationResultSender,
        ConnectorProperties connectorProperties
    ) {
        this.integrationResultSender = integrationResultSender;
        this.connectorProperties = connectorProperties;
    }

    @StreamListener(value = Channels.CHANNEL)
    public void handle(IntegrationRequest integrationRequest) throws InterruptedException {
        String var = integrationRequest.getIntegrationContext().getInBoundVariable("var");
        if (!"replay".equals(var)) {
            throw new RuntimeException("TestErrorConnector");
        } else {
            countDownLatch = new CountDownLatch(1);
        }

        logger.info("Processing integration request: {}", integrationRequest);

        Message<IntegrationResult> message = IntegrationResultBuilder
            .resultFor(integrationRequest, connectorProperties)
            .buildMessage();

        countDownLatch.await(10, TimeUnit.SECONDS);

        logger.info("Sending integration result: {}", message.getPayload());

        integrationResultSender.send(message);
    }
}
