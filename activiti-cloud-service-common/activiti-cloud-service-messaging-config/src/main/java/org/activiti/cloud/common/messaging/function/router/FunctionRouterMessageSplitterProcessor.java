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
package org.activiti.cloud.common.messaging.function.router;

import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.DESTINATION;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTE;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTE_CORRELATION_ID;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTING_CONTEXT;
import static org.springframework.integration.IntegrationMessageHeaderAccessor.CORRELATION_ID;

import java.util.Collection;
import java.util.Optional;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class FunctionRouterMessageSplitterProcessor extends AbstractMessageSplitter {

    private final ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties;

    public FunctionRouterMessageSplitterProcessor(
        ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties
    ) {
        this.functionRouterProperties = functionRouterProperties;
    }

    @Override
    protected @Nullable Object splitMessage(Message<?> message) {
        final var routingContext = Optional.ofNullable(
            message.getHeaders().get(ROUTING_CONTEXT, String.class)
        ).orElseThrow(() -> new MessageRejectedException(message, "missing routing context"));

        return Optional.ofNullable(message.getHeaders().get(DESTINATION, String.class))
            .map(functionRouterProperties.registrations(routingContext)::get)
            .stream()
            .flatMap(Collection::stream)
            .map(functionName ->
                MessageBuilder.fromMessage(message)
                    .setHeader(ROUTE_CORRELATION_ID, message.getHeaders().get(CORRELATION_ID))
                    .setHeader(ROUTE, functionName)
                    .build()
            )
            .toList();
    }
}
