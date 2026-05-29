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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.springframework.amqp.support.AmqpHeaders;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageDispatchingException;
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
        final var queuePrefix = Optional
            .ofNullable(messagingProperties.getRabbitmq().getPrefix())
            .map(prefix -> prefix.concat(groupPrefix))
            .orElse(groupPrefix);

        return declarable -> {
            if (declarable instanceof Queue queue) {
                Optional
                    .ofNullable(queue.getName())
                    .filter(it -> it.startsWith(queuePrefix))
                    .ifPresent(name -> queue.setLeaderLocator("client-local"));
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
            Optional
                .ofNullable(message.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class))
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
        BindingServiceProperties bindingServiceProperties
    ) {
        return new FunctionRouterHandler(
            messagingProperties,
            routingFunction,
            functionCatalog,
            functionExecutorSelector,
            messageContentTypeNormalizer,
            bindingServiceProperties
        );
    }

    private static class FunctionRouterHandler implements BiConsumer<Message<?>, String> {

        private static final Logger log = LoggerFactory.getLogger(FunctionRouterHandler.class);

        private final ActivitiCloudMessagingProperties messagingProperties;
        private final RoutingFunction routingFunction;
        private final FunctionCatalog functionCatalog;
        private final Function<Message<?>, ExecutorService> functionExecutorSelector;
        private final MessageContentTypeNormalizer messageContentTypeNormalizer;
        private final BindingServiceProperties bindingServiceProperties;

        FunctionRouterHandler(
            ActivitiCloudMessagingProperties messagingProperties,
            RoutingFunction routingFunction,
            FunctionCatalog functionCatalog,
            Function<Message<?>, ExecutorService> functionExecutorSelector,
            MessageContentTypeNormalizer messageContentTypeNormalizer,
            BindingServiceProperties bindingServiceProperties
        ) {
            this.messagingProperties = messagingProperties;
            this.routingFunction = routingFunction;
            this.functionCatalog = functionCatalog;
            this.functionExecutorSelector = functionExecutorSelector;
            this.messageContentTypeNormalizer = messageContentTypeNormalizer;
            this.bindingServiceProperties = bindingServiceProperties;
        }

        @Override
        public void accept(Message<?> message, String routingContext) {
            resolveRoutingKey(message)
                .map(messagingProperties.getFunctionRouter().registrations(routingContext)::get)
                .filter(Predicate.not(Collection::isEmpty))
                .ifPresentOrElse(
                    registrations -> dispatchToRegistrations(message, registrations),
                    () -> logUnroutableMessage(message, routingContext)
                );
        }

        private Optional<String> resolveRoutingKey(Message<?> message) {
            return Optional
                .ofNullable(message.getHeaders().get(FUNCTION_DESTINATION, String.class))
                .or(() -> Optional.ofNullable(message.getHeaders().get(CONNECTOR_TYPE, String.class)))
                .or(() ->
                    Optional
                        .ofNullable(messagingProperties.getRabbitmq().getPrefix())
                        .filter(Predicate.not(String::isBlank))
                        .flatMap(prefix ->
                            Optional
                                .ofNullable(message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class))
                                .filter(exchange -> exchange.startsWith(prefix))
                                .map(exchange -> exchange.substring(prefix.length()))
                        )
                )
                .or(() -> Optional.ofNullable(message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE, String.class)));
        }

        private void dispatchToRegistrations(Message<?> message, Collection<String> registrations) {
            List<AtomicReference<Future<?>>> cancellableFutures = new ArrayList<>();
            CompletableFuture<List<?>> allCompleted = submitAll(message, registrations, cancellableFutures);
            List<?> results = waitForCompletion(message, allCompleted, cancellableFutures);
            handleErrors(message, results);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private CompletableFuture<List<?>> submitAll(
            Message<?> message,
            Collection<String> registrations,
            List<AtomicReference<Future<?>>> cancellableFutures
        ) {
            CompletableFuture[] futures = registrations
                .stream()
                .map(functionRegistration -> buildFunctionRequest(message, functionRegistration))
                .map(functionRequest -> submitFunctionWithRetry(functionRequest, cancellableFutures))
                .toArray(CompletableFuture[]::new);

            return CompletableFuture
                .allOf(futures)
                .thenApply(v -> Stream.of(futures).map(CompletableFuture::join).toList());
        }

        private Message<?> buildFunctionRequest(Message<?> message, String functionRegistration) {
            String expectedContentType = messagingProperties.getFunctionRouter()
                .bindingNameFor(functionRegistration)
                .map(bindingName -> bindingServiceProperties.getBindings().get(bindingName))
                .map(BindingProperties::getContentType)
                .orElse(null);
            return MessageBuilder
                .fromMessage(messageContentTypeNormalizer.normalizeToExpected(message, expectedContentType))
                .setHeader(FunctionProperties.FUNCTION_DEFINITION, functionRegistration)
                .build();
        }

        private CompletableFuture<Map.Entry<String, Optional<Object>>> submitFunctionWithRetry(
            Message<?> functionRequest,
            List<AtomicReference<Future<?>>> cancellableFutures
        ) {
            final var functionRouter = messagingProperties.getFunctionRouter();
            AtomicReference<Future<?>> currentRawFuture = new AtomicReference<>();
            cancellableFutures.add(currentRawFuture);
            ExecutorService executor = functionExecutorSelector.apply(functionRequest);

            return supplyAsyncWithRetry(
                    () -> {
                        CompletableFuture<Object> cf = new CompletableFuture<>();
                        currentRawFuture.set(
                            executor.submit(() -> {
                                try {
                                    cf.complete(routingFunction.apply(functionRequest));
                                } catch (Exception t) {
                                    cf.completeExceptionally(t);
                                }
                            })
                        );
                        return cf;
                    },
                    functionRouter.getMaxRetries(),
                    functionRouter.getRetryInterval()
                )
                .thenApply(result -> {
                    var functionDefinition = functionRequest
                        .getHeaders()
                        .get(FunctionProperties.FUNCTION_DEFINITION, String.class);
                    log.debug(
                        "Function message request {} successfully routed to {}",
                        functionRequest,
                        functionDefinition
                    );
                    return Map.entry(functionDefinition, Optional.ofNullable(result));
                })
                .exceptionally(error -> {
                    var functionDefinition = functionRequest
                        .getHeaders()
                        .get(FunctionProperties.FUNCTION_DEFINITION, String.class);
                    log.error(
                        "Error routing message request {} to function registration {}",
                        functionRequest,
                        functionDefinition,
                        error
                    );
                    return Map.entry(functionDefinition, Optional.of(error));
                });
        }

        private List<?> waitForCompletion(
            Message<?> message,
            CompletableFuture<List<?>> completed,
            List<AtomicReference<Future<?>>> cancellableFutures
        ) {
            final var functionRouter = messagingProperties.getFunctionRouter();
            try {
                return completed.get(functionRouter.getProcessingTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelAll(cancellableFutures);
                throw new MessagingException(message, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException re ? re : new MessagingException(message, cause);
            } catch (TimeoutException e) {
                log.error(
                    "Processing timeout ({}) exceeded waiting for function router to handle message {}",
                    functionRouter.getProcessingTimeout(),
                    message
                );
                cancelAll(cancellableFutures);
                throw new MessagingException(message, e);
            }
        }

        private void cancelAll(List<AtomicReference<Future<?>>> cancellableFutures) {
            cancellableFutures.forEach(ref -> Optional.ofNullable(ref.get()).ifPresent(f -> f.cancel(true)));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void handleErrors(Message<?> message, List<?> results) {
            var errors = results
                .stream()
                .map(Map.Entry.class::cast)
                .filter(entry ->
                    Optional.class.cast(entry.getValue()).filter(Exception.class::isInstance).isPresent()
                )
                .map(entry -> Optional.class.cast(entry.getValue()).get())
                .toList();

            if (errors.isEmpty()) {
                log.debug("Successfully completed function route message request {}", message);
                return;
            }

            log.debug("Errors handling function route message request {}", errors);

            Optional
                .ofNullable(messagingProperties.getFunctionRouter().getErrorHandlerDefinition())
                .filter(StringUtils::hasText)
                .map(functionCatalog::lookup)
                .map(SimpleFunctionRegistry.FunctionInvocationWrapper.class::cast)
                .ifPresent(errorHandlerDefinition ->
                    errors
                        .stream()
                        .map(Throwable.class::cast)
                        .map(throwable ->
                            throwable instanceof CompletionException ce ? ce.getCause() : throwable
                        )
                        .map(exception -> {
                            if (exception instanceof MessagingException messagingException) {
                                return new ErrorMessage(messagingException, message);
                            } else {
                                return new ErrorMessage(new MessagingException(message, exception), message);
                            }
                        })
                        .forEach(errorHandlerDefinition::accept)
                );
        }

        private void logUnroutableMessage(Message<?> message, String routingContext) {
            final var destination = message.getHeaders().get(FUNCTION_DESTINATION, String.class);
            final var registration = Optional
                .ofNullable(destination)
                .map(it -> messagingProperties.getFunctionRouter().registrations(routingContext).get(it))
                .orElse(List.of());
            log.warn(
                "Unable to route message {} to destination '{}' for function registration '{}'",
                message,
                destination,
                registration
            );
        }
    }

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
                    Optional
                        .ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class))
                        .ifPresent(outputBinding -> {
                            messageChannel.addInterceptor(
                                new ChannelInterceptor() {
                                    @Override
                                    public Message<?> preSend(Message<?> message, MessageChannel channel) {
                                        return Optional
                                            .ofNullable(bindingServiceProperties.getBindings().get(beanName))
                                            .<Message<?>>map(binding ->
                                                MessageBuilder
                                                    .fromMessage(message)
                                                    .setHeader(FUNCTION_DESTINATION, binding.getDestination())
                                                    .build()
                                            )
                                            .orElse(message);
                                    }
                                }
                            );
                        });
                }
                return bean;
            }
        };
    }
}
