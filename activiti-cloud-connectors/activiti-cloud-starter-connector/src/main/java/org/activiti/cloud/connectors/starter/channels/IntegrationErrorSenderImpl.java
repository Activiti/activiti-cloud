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
package org.activiti.cloud.connectors.starter.channels;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;

import java.util.concurrent.atomic.AtomicBoolean;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationErrorSenderImpl implements IntegrationErrorSender, ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationErrorSenderImpl.class);

    private final StreamBridge streamBridge;
    private final IntegrationErrorChannelResolver resolver;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public IntegrationErrorSenderImpl(StreamBridge streamBridge, IntegrationErrorChannelResolver resolver) {
        this.streamBridge = streamBridge;
        this.resolver = resolver;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown.set(true);
    }

    @Override
    public void send(Message<IntegrationError> message) {
        if (shuttingDown.get()) {
            logger.warn(
                "Application is shutting down; skipping IntegrationError send so the request can be redelivered to another instance"
            );
            return;
        }

        IntegrationRequest request = message.getPayload().getIntegrationRequest();

        String destination = resolver.resolveDestination(request);

        streamBridge.send(
            destination,
            MessageBuilder.fromMessage(message).setHeader(FUNCTION_DESTINATION, destination).build()
        );
    }
}
