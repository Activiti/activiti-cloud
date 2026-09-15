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
import static org.springframework.integration.IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE;

import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class FunctionRouterMessageHandler implements GenericHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(FunctionRouterMessageHandler.class);

    private final RoutingFunction routingFunction;
    private final Function<MessageHeaders, String> functionRouterMessageDestinationSelector;

    private final ConcurrentMetadataStore metadataStore;
    private final FunctionRouterIdempotentInterceptorKeyStrategy idempotentInterceptorKeyStrategy;

    public FunctionRouterMessageHandler(
        RoutingFunction routingFunction,
        Function<MessageHeaders, String> functionRouterMessageDestinationSelector,
        ConcurrentMetadataStore metadataStore,
        FunctionRouterIdempotentInterceptorKeyStrategy idempotentInterceptorKeyStrategy
    ) {
        this.routingFunction = routingFunction;
        this.functionRouterMessageDestinationSelector = functionRouterMessageDestinationSelector;
        this.metadataStore = metadataStore;
        this.idempotentInterceptorKeyStrategy = idempotentInterceptorKeyStrategy;
    }

    @Override
    public @Nullable Object handle(Object payload, MessageHeaders headers) {
        final var headerAccessor = MessageHeaderAccessor.fromMessageHeaders(headers);

        logger.debug(
            "route {}, partition {}, payload: {}, headers: {}",
            headers.get(ROUTE),
            Thread.currentThread().getName(),
            payload,
            headers
        );

        if (headers.getOrDefault(DUPLICATE_MESSAGE, false) == Boolean.TRUE) {
            return new ErrorMessage(
                new MessageRejectedException(MessageBuilder.createMessage(payload, headers), "Duplicate message"),
                headers
            );
        }

        final var route = functionRouterMessageDestinationSelector.apply(headers);
        final var destination = headers.get(DESTINATION);

        headerAccessor.removeHeaders("amqp_*", "kafka_*", MessageHeaders.REPLY_CHANNEL, MessageHeaders.ERROR_CHANNEL);
        headerAccessor.setHeader(DESTINATION, destination);
        headerAccessor.setHeader(ROUTE, route);

        final var routeHeaders = headerAccessor.toMessageHeaders();
        final var routeMessage = MessageBuilder.withPayload(payload).copyHeaders(routeHeaders).build();

        try {
            final Object result = routingFunction.apply(routeMessage);

            return result instanceof Message<?> message
                ? message
                : result instanceof Throwable exception
                    ? new ErrorMessage(exception, routeMessage)
                    : MessageBuilder.withPayload(Optional.ofNullable(result)).copyHeaders(routeHeaders).build();
        } catch (Throwable throwable) {
            Optional.ofNullable(idempotentInterceptorKeyStrategy.processMessage(routeMessage)).ifPresent(
                metadataStore::remove
            );

            return new ErrorMessage(throwable, routeMessage);
        }
    }
}
