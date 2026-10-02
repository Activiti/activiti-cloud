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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerCircuitBreakerAdvice;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.ConcurrentReferenceHashMap;

public class StatefulRequestHandlerCircuitBreakerAdvice extends AbstractRequestHandlerAdvice {

    private final ConcurrentMap<Object, ConcurrentMetadataStore> adviceMap = new ConcurrentReferenceHashMap<>();

    private final MessageProcessor<String> messageProcessor;
    private final String routeHeaderName;
    private final String headerName;
    private final Function<Object, ConcurrentMetadataStore> concurrentMetadataStoreSupplier = key ->
        new SimpleMetadataStore();

    public StatefulRequestHandlerCircuitBreakerAdvice(
        MessageProcessor<String> messageProcessor,
        String headerName,
        String routeHeaderName
    ) {
        this.messageProcessor = messageProcessor;
        this.headerName = headerName;
        this.routeHeaderName = routeHeaderName;
    }

    @Override
    protected @Nullable Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
        final var header = Optional.ofNullable(message.getHeaders().get(this.headerName, String.class));

        if (header.isEmpty()) {
            return callback.execute();
        }

        final var key = header.get();
        final var value = messageProcessor.processMessage(message);
        final var route = message.getHeaders().get(routeHeaderName, String.class);
        final var entry = Map.entry(headerName, key);

        final var metadataMap = adviceMap.computeIfAbsent(route, concurrentMetadataStoreSupplier);
        final var metadata = new AtomicReference<>(metadataMap.get(key));

        if (metadata.get() == null) {
            metadataMap.putIfAbsent(key, value);
            metadata.set(value);
        }

        if (!Objects.equals(metadata.get(), value)) {
            throw new RequestHandlerCircuitBreakerAdvice.CircuitBreakerOpenException(
                message,
                "Circuit breaker %s is open for %s".formatted(route, entry)
            );
        }
        try {
            Object result = callback.execute();

            Optional.ofNullable(result)
                .filter(ErrorMessage.class::isInstance)
                .map(ErrorMessage.class::cast)
                .map(ErrorMessage::getPayload)
                .filter(Predicate.not(MessageRejectedException.class::isInstance))
                .ifPresentOrElse(
                    error -> {
                        logger.warn(() ->
                            "Open circuit breaker %s for %s due to error: %s".formatted(
                                route,
                                entry,
                                error.getMessage()
                            )
                        );
                    },
                    () -> {
                        logger.debug(() -> "Closing circuit breaker %s for %s".formatted(route, entry));
                        metadataMap.remove(key);
                    }
                );

            return result;
        } catch (Exception error) {
            throw error;
        }
    }
}
