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

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class IntegrationResultSenderImpl implements IntegrationResultSender {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationResultSenderImpl.class);

    private final StreamBridge streamBridge;
    private final IntegrationResultChannelResolver resolver;

    public IntegrationResultSenderImpl(StreamBridge streamBridge, IntegrationResultChannelResolver resolver) {
        this.streamBridge = streamBridge;
        this.resolver = resolver;
    }

    @Override
    public void send(Message<IntegrationResult> message) {
        IntegrationRequest request = message.getPayload().getIntegrationRequest();

        String destination = resolver.resolveDestination(request);

        logger.info(
            "FN-CONNECTOR-TRACE sending IntegrationResult integrationContextId={} destination={}",
            request.getIntegrationContext().getId(),
            destination
        );
        streamBridge.send(
            destination,
            MessageBuilder.fromMessage(message).setHeader(FUNCTION_DESTINATION, destination).build()
        );
    }
}
