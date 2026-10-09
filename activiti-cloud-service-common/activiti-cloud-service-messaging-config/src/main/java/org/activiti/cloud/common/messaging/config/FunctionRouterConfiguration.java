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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dispatcher.AggregateMessageDeliveryException;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingConsumer;

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
        Function<Message<?>, ExecutorService> functionExecutorSelector,
        MessageContentTypeNormalizer messageContentTypeNormalizer,
        BindingServiceProperties bindingServiceProperties,
        Function<String, Optional<Consumer<ErrorMessage>>> functionRouterErrorHandlerDefinitionResolver
    ) {
        return new FunctionRouterMessageHandler(
            routingFunction,
            messagingProperties,
            functionExecutorSelector,
            messageContentTypeNormalizer,
            bindingServiceProperties,
            functionRouterErrorHandlerDefinitionResolver
        );
    }

    @Bean
    Function<String, Optional<Consumer<ErrorMessage>>> functionRouterErrorHandlerDefinitionResolver(
        ActivitiCloudMessagingProperties messagingProperties,
        FunctionCatalog functionCatalog,
        Environment environment
    ) {
        final var functionRouter = messagingProperties.getFunctionRouter();

        return bindingName ->
            Optional.of(functionRouter.bindings().get(bindingName))
                .map(BindingProperties::getErrorHandlerDefinition)
                .or(() ->
                    Optional.ofNullable(
                        environment.getProperty("spring.cloud.stream.default.error-handler-definition", String.class)
                    )
                )
                .filter(StringUtils::hasText)
                .map(functionCatalog::lookup)
                .filter(Consumer.class::isInstance)
                .<Consumer<ErrorMessage>>map(Consumer.class::cast);
    }

    @Bean
    Consumer<ErrorMessage> functionRouterErrorMessageHandler() {
        return new Consumer<ErrorMessage>() {
            @Override
            public void accept(ErrorMessage errorMessage) {
                final var originalMessage = errorMessage.getOriginalMessage();

                findCause(errorMessage, AggregateMessageDeliveryException.class)
                    .map(cause -> new MessageDeliveryException(originalMessage, cause.getMessage(), cause))
                    .or(() ->
                        findCause(errorMessage, TimeoutException.class).map(cause ->
                            new MessageTimeoutException(originalMessage, cause.getMessage(), cause)
                        )
                    )
                    .ifPresentOrElse(ThrowingConsumer.of(this::throwException), () ->
                        log.warn("Unresolved function router error message: {}", errorMessage)
                    );
            }

            @SuppressWarnings("java:S112")
            private void throwException(Exception exception) throws Exception {
                throw exception;
            }

            private <T extends Throwable> Optional<Throwable> findCause(
                ErrorMessage errorMessage,
                Class<T> targetType
            ) {
                Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                var throwable = errorMessage.getPayload();

                while (throwable != null && seen.add(throwable)) {
                    if (targetType.isInstance(throwable)) {
                        return Optional.of(throwable);
                    }
                    throwable = throwable.getCause();
                }
                return Optional.empty();
            }
        };
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
                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class)).ifPresent(
                        outputBinding -> {
                            messageChannel.addInterceptor(
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
                            );
                        }
                    );
                }
                return bean;
            }
        };
    }
}
