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

import static org.activiti.cloud.common.messaging.config.CompletableFutureRetry.supplyAsyncWithRetry;

import com.rabbitmq.client.Channel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * Routes connector messages to per-connector executors under manual acknowledgment: the router acks
 * only once the fire-and-forget handoff to the executor has genuinely completed, and redelivers
 * delivery failures rather than dropping the work.
 *
 * <p>A destination may fan out to several registrations sharing a single broker delivery. Delivery
 * failures are handled so that already-succeeded registrations are not re-run: when all registrations
 * fail the whole message is nack+requeued; when only some fail, just those are re-published pinned via
 * the {@link #TARGET_REGISTRATIONS} header and the original message is acked. If the re-publish cannot
 * be sent, the router falls back to requeueing the whole message so nothing is lost, so consumers
 * sharing a destination should still tolerate occasional redelivery.
 */
@AutoConfiguration(
    before = InputBindingConfiguration.class,
    after = { BinderFactoryAutoConfiguration.class, ActivitiMessagingDestinationsAutoConfiguration.class }
)
@ConditionalOnProperty("activiti.cloud.messaging.function-router.enabled")
public class FunctionRouterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterConfiguration.class);

    public static final String FUNCTION_DESTINATION = "spring.cloud.function.destination";
    public static final String FUNCTION_ROUTER_INPUT = "functionRouterInput";
    public static final String FUNCTION_ROUTER_ANONYMOUS_INPUT = "functionRouterAnonymousInput";
    public static final String CONNECTOR_TYPE = "connectorType";
    public static final String TARGET_REGISTRATIONS = "functionRouterTargetRegistrations";
    private static final String QUEUE_MASTER_LOCATOR = "x-queue-master-locator";

    @Bean
    ApplicationRunner functionRouterConfigurationApplicationRunner(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return args -> log.warn("Function Router has been initialized: {}", messagingProperties.getFunctionRouter());
    }

    @Configuration
    static class FunctionRouterChannels {

        @InputBinding(FUNCTION_ROUTER_INPUT)
        SubscribableChannel functionRouterInput() {
            return MessageChannels.publishSubscribe(FUNCTION_ROUTER_INPUT).getObject();
        }

        @InputBinding(FUNCTION_ROUTER_ANONYMOUS_INPUT)
        SubscribableChannel functionRouterAnonymousInput() {
            return MessageChannels.publishSubscribe(FUNCTION_ROUTER_ANONYMOUS_INPUT).getObject();
        }
    }

    @Bean
    DeclarableCustomizer functionRouterAnonymousQueueCustomizer(ActivitiCloudMessagingProperties messagingProperties) {
        final var groupPrefix = messagingProperties.getFunctionRouter().groupPrefix();
        final var queuePrefix = Optional.ofNullable(messagingProperties.getRabbitmq().getPrefix())
            .map(prefix -> prefix.concat(groupPrefix))
            .orElse(groupPrefix);

        return declarable -> {
            if (declarable instanceof Queue queue) {
                Optional.ofNullable(queue.getName())
                    .filter(it -> it.startsWith(queuePrefix))
                    .ifPresent(name ->
                        queue.addArgument(QUEUE_MASTER_LOCATOR, QueueBuilder.LeaderLocator.clientLocal.getValue())
                    );
            }

            return declarable;
        };
    }

    @Bean
    @FunctionBinding(input = FUNCTION_ROUTER_INPUT)
    Consumer<Message<?>> functionRouterConsumer(BiConsumer<Message<?>, String> functionRouterMessageHandler) {
        return message -> functionRouterMessageHandler.accept(message, FUNCTION_ROUTER_INPUT);
    }

    @Bean
    @FunctionBinding(input = FUNCTION_ROUTER_ANONYMOUS_INPUT)
    Consumer<Message<?>> functionRouterAnonymousConsumer(BiConsumer<Message<?>, String> functionRouterMessageHandler) {
        return message -> functionRouterMessageHandler.accept(message, FUNCTION_ROUTER_ANONYMOUS_INPUT);
    }

    @Bean
    @ConditionalOnMissingBean
    Function<String, ExecutorService> functionRouterExecutorFactory(
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        return new FunctionRouterExecutorFactory(messagingProperties.getFunctionRouter().getRequestTimeout());
    }

    @Bean
    Function<Message<?>, String> functionRegistrationSelector() {
        return message ->
            Optional.ofNullable(message.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class))
                .filter(Predicate.not(String::isBlank))
                .orElseThrow(() ->
                    new MessageDispatchingException(
                        String.format("Message header %s is required", FunctionProperties.FUNCTION_DEFINITION)
                    )
                );
    }

    @Bean
    Function<Message<?>, ExecutorService> functionExecutorSelector(
        Function<Message<?>, String> functionRegistrationSelector,
        Function<String, ExecutorService> functionRouterExecutorFactory
    ) {
        return message -> functionRegistrationSelector.andThen(functionRouterExecutorFactory).apply(message);
    }

    @Bean
    BiConsumer<Message<?>, String> functionRouterMessageHandler(
        RoutingFunction routingFunction,
        ActivitiCloudMessagingProperties messagingProperties,
        FunctionCatalog functionCatalog,
        Function<Message<?>, ExecutorService> functionExecutorSelector,
        MessageContentTypeNormalizer messageContentTypeNormalizer,
        BindingServiceProperties bindingServiceProperties,
        ObjectProvider<StreamBridge> streamBridgeProvider
    ) {
        final var functionRouter = messagingProperties.getFunctionRouter();

        return (message, routingContext) -> {
            final var resolvedDestination = Optional.ofNullable(
                message.getHeaders().get(FUNCTION_DESTINATION, String.class)
            )
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
                .or(() -> Optional.ofNullable(message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class)));

            resolvedDestination
                .map(destination ->
                    Map.entry(
                        destination,
                        filterToTargetRegistrations(
                            message,
                            messagingProperties.getFunctionRouter().registrations(routingContext).get(destination)
                        )
                    )
                )
                .filter(entry -> !entry.getValue().isEmpty())
                .ifPresentOrElse(
                    entry -> {
                        final var destination = entry.getKey();
                        final var registrations = entry.getValue();
                        BiFunction<Message<?>, String, Message<?>> toFunctionRequest = (
                            functionMessage,
                            functionRegistration
                        ) -> {
                            String expectedContentType = functionRouter
                                .bindingNameFor(functionRegistration)
                                .map(bindingName -> bindingServiceProperties.getBindings().get(bindingName))
                                .map(BindingProperties::getContentType)
                                .orElse(null);
                            return MessageBuilder.fromMessage(
                                messageContentTypeNormalizer.normalizeToExpected(functionMessage, expectedContentType)
                            )
                                .setHeader(FunctionProperties.FUNCTION_DEFINITION, functionRegistration)
                                // Manual ack carries a live, non-serializable acknowledgment handle on
                                // the message: the AMQP channel/delivery-tag on the classic RabbitMQ
                                // binder, or an AcknowledgmentCallback on binders that provide one.
                                // Neither must leak into the routed business message: a handler that
                                // persists it (e.g. the messages-service's JdbcMessageStore aggregator
                                // serializes the message) would fail to serialize the handle. The outer
                                // message retains them for acknowledge()/negativelyAcknowledgeAndRequeue().
                                .removeHeader(AmqpHeaders.CHANNEL)
                                .removeHeader(AmqpHeaders.DELIVERY_TAG)
                                .removeHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK)
                                .removeHeader(TARGET_REGISTRATIONS)
                                .build();
                        };

                        Function<Message<?>, CompletableFuture<Object>> submitFunctionRequest = functionRequest -> {
                            // supplyAsync() submits synchronously, so a throwing
                            // RejectedExecutionHandler (queue full / executor shutting down) would
                            // escape here instead of failing the future - wrap it so the retry and
                            // exceptionally() handling below still apply.
                            try {
                                return CompletableFuture.supplyAsync(
                                    () -> routingFunction.apply(functionRequest),
                                    functionExecutorSelector.apply(functionRequest)
                                );
                            } catch (RuntimeException e) {
                                return CompletableFuture.failedFuture(e);
                            }
                        };

                        var functions = registrations
                            .stream()
                            .map(functionRegistration -> {
                                var functionRequest = toFunctionRequest.apply(message, functionRegistration);
                                return supplyAsyncWithRetry(
                                    () -> submitFunctionRequest.apply(functionRequest),
                                    functionRouter.getMaxRetries(),
                                    functionRouter.getRetryInterval()
                                )
                                    .thenApply(result -> {
                                        log.debug(
                                            "Function message request {} successfully routed to {}",
                                            functionRequest,
                                            functionRegistration
                                        );
                                        return new RegistrationOutcome(functionRegistration, Optional.empty(), false);
                                    })
                                    .exceptionally(error -> {
                                        var cause = error instanceof CompletionException ce ? ce.getCause() : error;

                                        // Delivery failures are redelivered (only the failed
                                        // registration, see below), not reported to the service task
                                        // as errors. debug, not warn: an unclearing failure loops with
                                        // no backoff and would flood the logs.
                                        if (
                                            cause instanceof RejectedExecutionException ||
                                            cause instanceof RequeueDeliveryException
                                        ) {
                                            log.debug(
                                                "Delivery failure for registration {} - will be redelivered",
                                                functionRegistration,
                                                error
                                            );
                                            return new RegistrationOutcome(
                                                functionRegistration,
                                                Optional.empty(),
                                                true
                                            );
                                        }

                                        log.error(
                                            "Error routing message request {} to function registration {}",
                                            functionRequest,
                                            functionRegistration,
                                            error
                                        );
                                        return new RegistrationOutcome(functionRegistration, Optional.of(error), false);
                                    });
                            })
                            .toArray(CompletableFuture[]::new);

                        CompletableFuture.allOf(functions)
                            .thenApply(v ->
                                Stream.of(functions)
                                    .map(future -> (RegistrationOutcome) future.join())
                                    .toList()
                            )
                            .thenAccept(outcomes -> {
                                var executionErrors = outcomes
                                    .stream()
                                    .map(RegistrationOutcome::executionError)
                                    .flatMap(Optional::stream)
                                    .toList();

                                if (!executionErrors.isEmpty()) {
                                    log.debug("Errors handling function route message request {}", executionErrors);

                                    Optional.ofNullable(
                                        messagingProperties.getFunctionRouter().getErrorHandlerDefinition()
                                    )
                                        .filter(StringUtils::hasText)
                                        .map(functionCatalog::lookup)
                                        .map(SimpleFunctionRegistry.FunctionInvocationWrapper.class::cast)
                                        .ifPresent(errorHandlerDefinition ->
                                            executionErrors
                                                .stream()
                                                .map(CompletionException.class::cast)
                                                .map(CompletionException::getCause)
                                                .map(exception -> {
                                                    if (exception instanceof MessagingException messagingException) {
                                                        return new ErrorMessage(messagingException, message);
                                                    } else {
                                                        return new ErrorMessage(
                                                            new MessagingException(message, exception),
                                                            message
                                                        );
                                                    }
                                                })
                                                .forEach(errorHandlerDefinition)
                                        );
                                }

                                var failedRegistrations = outcomes
                                    .stream()
                                    .filter(RegistrationOutcome::deliveryFailure)
                                    .map(RegistrationOutcome::registration)
                                    .toList();

                                if (failedRegistrations.isEmpty()) {
                                    // every registration reached a final outcome (success or an error
                                    // already reported): ack so the broker does not redeliver.
                                    log.debug("Successfully completed function route message request {}", message);
                                    acknowledge(message);
                                } else if (failedRegistrations.size() == outcomes.size()) {
                                    // no registration succeeded, so requeueing the whole message cannot
                                    // re-run already-done work: nack+requeue and let the broker redeliver.
                                    log.debug("Delivery failure for all registrations, message will be requeued");
                                    negativelyAcknowledgeAndRequeue(message);
                                } else if (
                                    redeliverFailedRegistrations(
                                        streamBridgeProvider,
                                        destination,
                                        message,
                                        failedRegistrations
                                    )
                                ) {
                                    // partial failure: re-publish only the failed registrations (pinned)
                                    // so the ones that already succeeded do not re-run, then ack the original.
                                    acknowledge(message);
                                } else {
                                    // could not re-publish (broker/producer unavailable): requeue the
                                    // whole message so nothing is lost, at the cost of re-running the
                                    // registrations that already succeeded.
                                    negativelyAcknowledgeAndRequeue(message);
                                }
                            })
                            .exceptionally(unexpectedError -> {
                                log.warn(
                                    "Unexpected error completing function route message request {}, requeueing",
                                    message,
                                    unexpectedError
                                );
                                negativelyAcknowledgeAndRequeue(message);
                                return null;
                            });
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

                        // no registration for this destination: requeuing would loop forever, so ack
                        acknowledge(message);
                    }
                );
        };
    }

    /**
     * Acks the message once processing has genuinely completed. Prefers the binder-neutral
     * {@link AcknowledgmentCallback} when the active binder provides one (e.g. Kafka); falls back to
     * the RabbitMQ channel/delivery-tag when it does not (the classic AMQP binder exposes only
     * those). A no-op when neither is present, so it is safe to call regardless of ack mode.
     */
    static void acknowledge(Message<?> message) {
        final var callback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
        if (callback != null) {
            callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
            return;
        }

        withChannelAndDeliveryTag(message, (channel, deliveryTag) -> {
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.warn("Failed to acknowledge message {}", message, e);
            }
        });
    }

    /**
     * Negatively acks the message with requeue so the broker redelivers it. Prefers the
     * binder-neutral {@link AcknowledgmentCallback.Status#REQUEUE} when available; falls back to a
     * RabbitMQ nack with {@code requeue=true} otherwise. A no-op when neither is present.
     */
    static void negativelyAcknowledgeAndRequeue(Message<?> message) {
        final var callback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
        if (callback != null) {
            callback.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
            return;
        }

        withChannelAndDeliveryTag(message, (channel, deliveryTag) -> {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception e) {
                log.warn("Failed to negatively acknowledge message {}", message, e);
            }
        });
    }

    private static void withChannelAndDeliveryTag(Message<?> message, ObjLongConsumer<Channel> action) {
        var channel = message.getHeaders().get(AmqpHeaders.CHANNEL, Channel.class);
        var deliveryTag = message.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class);

        if (channel != null && deliveryTag != null) {
            action.accept(channel, deliveryTag);
        }
    }

    /**
     * Restricts the destination's registrations to those pinned by the {@link #TARGET_REGISTRATIONS}
     * header when present (a redelivery of specific failed registrations); otherwise returns all of
     * them. Never returns {@code null}: an empty list means there is nothing for this router to run
     * (e.g. a pinned redelivery that reached another application which does not host that
     * registration), so the caller acks it as a no-op.
     */
    static List<String> filterToTargetRegistrations(Message<?> message, List<String> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return List.of();
        }

        final var targetRegistrations = message.getHeaders().get(TARGET_REGISTRATIONS, String.class);
        if (targetRegistrations == null || targetRegistrations.isBlank()) {
            return registrations;
        }

        final var targets = List.of(targetRegistrations.split(","));
        return registrations.stream().filter(targets::contains).toList();
    }

    /**
     * Re-publishes only the registrations that hit a delivery failure - pinned via the
     * {@link #TARGET_REGISTRATIONS} header - back to the source destination, so the broker redelivers
     * just those. The registrations that already succeeded are not re-run. Returns {@code true} once
     * the redelivery has been sent, {@code false} if it could not be (so the caller can requeue the
     * whole message rather than lose the failed work).
     */
    static boolean redeliverFailedRegistrations(
        ObjectProvider<StreamBridge> streamBridgeProvider,
        String destination,
        Message<?> message,
        List<String> failedRegistrations
    ) {
        try {
            final var redelivery = MessageBuilder.fromMessage(message)
                .setHeader(FUNCTION_DESTINATION, destination)
                .setHeader(TARGET_REGISTRATIONS, String.join(",", failedRegistrations))
                // never carry the original delivery's live, non-serializable ack handle into the copy
                .removeHeader(AmqpHeaders.CHANNEL)
                .removeHeader(AmqpHeaders.DELIVERY_TAG)
                .removeHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK)
                .build();

            final var sent = streamBridgeProvider.getObject().send(destination, redelivery);
            if (!sent) {
                log.warn(
                    "Re-publish for redelivery of registrations {} to '{}' returned false",
                    failedRegistrations,
                    destination
                );
            }
            return sent;
        } catch (Exception e) {
            log.warn(
                "Failed to re-publish registrations {} to '{}' for redelivery; requeueing whole message",
                failedRegistrations,
                destination,
                e
            );
            return false;
        }
    }

    private record RegistrationOutcome(
        String registration,
        Optional<Throwable> executionError,
        boolean deliveryFailure
    ) {}

    @Bean
    MessageRoutingCallback functionRouterMessageRoutingCallback() {
        return new MessageRoutingCallback() {
            @Override
            public String routingResult(Message<?> message) {
                return message.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class);
            }
        };
    }

    @Bean
    public BeanPostProcessor outputBindingChannelPostProcessor(
        @Autowired DefaultListableBeanFactory beanFactory,
        @Autowired BindingServiceProperties bindingServiceProperties
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DirectChannel messageChannel) {
                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class)).ifPresent(
                        outputBinding -> messageChannel.addInterceptor(
                            new ChannelInterceptor() {
                                @Override
                                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                                    return Optional.ofNullable(bindingServiceProperties.getBindings().get(beanName))
                                        .<Message<?>>map(binding ->
                                            MessageBuilder.fromMessage(message)
                                                .setHeader(FUNCTION_DESTINATION, binding.getDestination())
                                                .build()
                                        )
                                        .orElse(message);
                                }
                            }
                        )
                    );
                }
                return bean;
            }
        };
    }
}
