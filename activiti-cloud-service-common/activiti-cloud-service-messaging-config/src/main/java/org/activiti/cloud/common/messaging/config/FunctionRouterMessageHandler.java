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

package org.activiti.cloud.common.messaging.config;

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.CONNECTOR_TYPE;
import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.dispatcher.AggregateMessageDeliveryException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

public class FunctionRouterMessageHandler implements BiConsumer<Message<?>, String> {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterMessageHandler.class);

    private final RoutingFunction routingFunction;
    private final ActivitiCloudMessagingProperties messagingProperties;
    private final Function<Message<?>, ExecutorService> functionExecutorSelector;
    private final MessageContentTypeNormalizer messageContentTypeNormalizer;
    private final BindingServiceProperties bindingServiceProperties;
    private final Function<String, Optional<Consumer<ErrorMessage>>> functionRouterErrorHandlerDefinitionResolver;
    private final ActivitiCloudMessagingProperties.FunctionRouterProperties functionRouter;

    public FunctionRouterMessageHandler(
        RoutingFunction routingFunction,
        ActivitiCloudMessagingProperties messagingProperties,
        Function<Message<?>, ExecutorService> functionExecutorSelector,
        MessageContentTypeNormalizer messageContentTypeNormalizer,
        BindingServiceProperties bindingServiceProperties,
        Function<String, Optional<Consumer<ErrorMessage>>> functionRouterErrorHandlerDefinitionResolver
    ) {
        this.routingFunction = routingFunction;
        this.messagingProperties = messagingProperties;
        this.functionExecutorSelector = functionExecutorSelector;
        this.messageContentTypeNormalizer = messageContentTypeNormalizer;
        this.bindingServiceProperties = bindingServiceProperties;
        this.functionRouterErrorHandlerDefinitionResolver = functionRouterErrorHandlerDefinitionResolver;
        this.functionRouter = messagingProperties.getFunctionRouter();
    }

    @Override
    public void accept(Message<?> message, String routingContext) {
        Optional.ofNullable(message.getHeaders().get(FUNCTION_DESTINATION, String.class))
            .or(() -> Optional.ofNullable(message.getHeaders().get(CONNECTOR_TYPE, String.class)))
            .or(() ->
                Optional.ofNullable(messagingProperties.getRabbitmq().getPrefix())
                    .filter(Predicate.not(String::isBlank))
                    .flatMap(prefix ->
                        Optional.ofNullable(message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class))
                            .filter(exchange -> exchange.startsWith(prefix))
                            .map(exchange -> exchange.substring(prefix.length()))
                    )
            )
            .or(() -> Optional.ofNullable(message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class)))
            .map(messagingProperties.getFunctionRouter().registrations(routingContext)::get)
            .filter(Predicate.not(Collection::isEmpty))
            .ifPresentOrElse(
                registrations -> {
                    Function<Message<?>, String> resolveFunctionDefinition = functionMessage ->
                        functionMessage.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class);
                    BiFunction<Message<?>, String, Message<?>> toFunctionRequest = (
                        functionMessage,
                        functionRegistration
                    ) -> {
                        String expectedContentType = messagingProperties
                            .getFunctionRouter()
                            .bindingNameFor(functionRegistration)
                            .map(bindingName -> bindingServiceProperties.getBindings().get(bindingName))
                            .map(BindingProperties::getContentType)
                            .orElse(null);
                        return MessageBuilder.fromMessage(
                            messageContentTypeNormalizer.normalizeToExpected(functionMessage, expectedContentType)
                        )
                            .setHeader(FunctionProperties.FUNCTION_DEFINITION, functionRegistration)
                            .build();
                    };

                    Function<Message<?>, CompletableFuture<Object>> routingFunctionFuture = request -> {
                        final CompletableFuture<Object> future = new CompletableFuture<>();
                        try {
                            functionExecutorSelector.apply(request).execute(() -> {
                                try {
                                    future.complete(routingFunction.apply(request));
                                } catch (Throwable ex) {
                                    future.completeExceptionally(ex);
                                }
                            });
                        } catch (Exception exception) {
                            future.completeExceptionally(exception);
                        }
                        return future;
                    };

                    var functions = registrations
                        .stream()
                        .map(functionRegistration -> toFunctionRequest.apply(message, functionRegistration))
                        .map(functionRequest ->
                            routingFunctionFuture
                                .apply(functionRequest)
                                .thenApply(result -> {
                                    var functionDefinition = resolveFunctionDefinition.apply(functionRequest);
                                    log.debug(
                                        "Function message request {} successfully routed to {}",
                                        functionRequest,
                                        functionDefinition
                                    );
                                    return Map.entry(functionDefinition, Optional.ofNullable(result));
                                })
                                .exceptionally(error -> {
                                    var functionDefinition = resolveFunctionDefinition.apply(functionRequest);
                                    log.warn(
                                        "Error routing message request {} to function registration {}",
                                        functionRequest,
                                        functionDefinition,
                                        error
                                    );
                                    return Map.entry(functionDefinition, Optional.of(error));
                                })
                        )
                        .toList();

                    CompletableFuture.allOf(functions.toArray(CompletableFuture[]::new))
                        .thenApply(v -> functions.stream().map(CompletableFuture::join).toList())
                        .thenAccept(results -> {
                            final var errors = results
                                .stream()
                                .filter(entry ->
                                    entry.getValue().filter(CompletionException.class::isInstance).isPresent()
                                )
                                .map(entry ->
                                    Map.entry(
                                        entry.getKey(),
                                        entry
                                            .getValue()
                                            .map(CompletionException.class::cast)
                                            .map(CompletionException::getCause)
                                            .get()
                                    )
                                )
                                .map(entry ->
                                    messagingProperties
                                        .getFunctionRouter()
                                        .bindingNameFor(entry.getKey())
                                        .map(bindingName -> Map.entry(bindingName, entry.getValue()))
                                        .orElse(entry)
                                )
                                .toList();

                            if (!errors.isEmpty()) {
                                log.debug("Errors handling function route message request {}", errors);
                                final Function<ErrorMessage, Consumer<ErrorMessage>> fallbackErrorHandler =
                                    errorMessage -> {
                                        throw new RuntimeException(errorMessage.getPayload());
                                    };

                                final var errorHandlingResults = errors
                                    .stream()
                                    .filter(Objects::nonNull)
                                    .map(entry -> {
                                        final var errorMessage = errorMessage(message, entry.getValue());
                                        final var errorHandlerDefinition = functionRouterErrorHandlerDefinitionResolver
                                            .apply(entry.getKey())
                                            .orElseGet(() -> fallbackErrorHandler.apply(errorMessage));

                                        try {
                                            return CompletableFuture.runAsync(
                                                () -> errorHandlerDefinition.accept(errorMessage),
                                                Runnable::run
                                            );
                                        } catch (Exception e) {
                                            return CompletableFuture.failedFuture(e);
                                        }
                                    })
                                    .filter(CompletableFuture::isCompletedExceptionally)
                                    .map(CompletableFuture::exceptionNow)
                                    .map(RuntimeException::new)
                                    .toList();

                                if (!errorHandlingResults.isEmpty()) {
                                    throw new AggregateMessageDeliveryException(
                                        message,
                                        "Function router result errors",
                                        errorHandlingResults
                                    );
                                }
                            } else {
                                log.debug("Successfully completed function route message request {}", message);
                            }
                        })
                        .orTimeout(functionRouter.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .join();
                },
                () -> {
                    final var destination = message.getHeaders().get(FUNCTION_DESTINATION, String.class);

                    final var registration = Optional.ofNullable(destination)
                        .map(it -> messagingProperties.getFunctionRouter().registrations(routingContext).get(it))
                        .orElse(List.of());

                    log.warn(
                        "Unable to route message {} to destination '{}' for function registration '{}'",
                        message,
                        destination,
                        registration
                    );
                }
            );
    }

    private ErrorMessage errorMessage(Message<?> message, Throwable throwable) {
        return Optional.of(throwable)
            .filter(MessagingException.class::isInstance)
            .map(messagingException -> new ErrorMessage(messagingException, message))
            .orElseGet(() -> new ErrorMessage(new MessagingException(message, throwable), message));
    }
}
