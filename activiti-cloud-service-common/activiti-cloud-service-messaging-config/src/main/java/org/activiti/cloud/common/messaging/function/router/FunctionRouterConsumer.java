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

import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.FUNCTION_DESTINATION;
import static org.activiti.cloud.common.messaging.function.router.FunctionRouterMessageHeaders.ROUTING_CONTEXT;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.RecoveryCallback;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class FunctionRouterConsumer implements Function<Flux<Message<?>>, Mono<Void>> {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterConsumer.class);
    private static final Integer PREFETCH_ONE = 1;

    private final FunctionRouterGateway functionRouterGateway;
    private final Function<Message<?>, String> destinationResolver;
    private final List<String> destinations;
    private final RecoveryCallback<Object> recoveryCallback;
    private final Supplier<String> routingContextProvider;
    private final ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties;

    public FunctionRouterConsumer(
        FunctionRouterGateway functionRouterGateway,
        Function<Message<?>, String> destinationResolver,
        Function<String, List<String>> destinationsProvider,
        RecoveryCallback<Object> recoveryCallback,
        Supplier<String> routingContextProvider,
        ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouterProperties
    ) {
        this.functionRouterGateway = functionRouterGateway;
        this.destinationResolver = destinationResolver;
        this.functionRouterProperties = functionRouterProperties;
        this.destinations = destinationsProvider.apply(routingContextProvider.get());
        this.recoveryCallback = recoveryCallback;
        this.routingContextProvider = routingContextProvider;
    }

    @Override
    public Mono<Void> apply(Flux<Message<?>> messageFlux) {
        return messageFlux
            .map(message -> Tuples.of(destinationResolver.apply(message), message))
            .onErrorContinue((throwable, item) -> {
                if (item instanceof Message<?> message) {
                    final var destination = message.getHeaders().get(FUNCTION_DESTINATION, String.class);

                    final var registrations = Optional.ofNullable(destination)
                        .map(it -> functionRouterProperties.registrations(routingContextProvider.get()).get(it))
                        .orElse(List.of());

                    log.warn(
                        "Unable to route message {} to destination '{}' for function registrations '{}'",
                        message,
                        destination,
                        registrations
                    );
                } else {
                    log.error("Dropping item {} due to resolver exception", item, throwable);
                }
            })
            .groupBy(Tuple2::getT1, Tuple2::getT2)
            .flatMap(
                destination ->
                    destination.concatMap(message ->
                        Mono.defer(() -> handleMessage(destination.key(), message))
                            .retry(3)
                            .onErrorResume(error -> onErrorResume(message, error))
                    ),
                destinations.size(),
                PREFETCH_ONE
            )
            .then();
    }

    private Mono<Void> handleMessage(String destination, Message<?> message) {
        final var result = functionRouterGateway.forwardTo(
            destination,
            MessageBuilder.fromMessage(message).setHeader(ROUTING_CONTEXT, routingContextProvider.get()).build()
        );

        if (result.hasErrors()) {
            final var causes = result.getCauses();

            if (!causes.isEmpty()) {
                throw new FunctionRouterMessageDeliveryException(message, "Function router result errors", causes);
            }
        }

        return Mono.empty();
    }

    private Mono<Void> onErrorResume(Message<?> message, Throwable error) {
        final var context = ErrorMessageUtils.getAttributeAccessor(message, null);

        recoveryCallback.recover(context, error);

        return Mono.empty();
    }
}
