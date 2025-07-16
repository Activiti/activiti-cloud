/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

@AutoConfiguration(
    before = InputBindingConfiguration.class,
    after = { BinderFactoryAutoConfiguration.class, ActivitiMessagingDestinationsAutoConfiguration.class }
)
@ConditionalOnProperty("activiti.cloud.messaging.function-router.enabled")
public class FunctionRouterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterConfiguration.class);

    public static final String FUNCTION_DESTINATION = "spring.cloud.function.destination";
    public static final String FUNCTION_ROUTER_INPUT = "functionRouterInput";

    @Configuration
    static class FunctionRouterChannels {

        @InputBinding(FUNCTION_ROUTER_INPUT)
        SubscribableChannel functionRouterInput() {
            return MessageChannels.publishSubscribe(FUNCTION_ROUTER_INPUT).getObject();
        }
    }

    @Bean
    @FunctionBinding(input = FUNCTION_ROUTER_INPUT)
    Consumer<Message<?>> functionRouterConsumer(
        RoutingFunction routingFunction,
        ActivitiCloudMessagingProperties messagingProperties
    ) {
        final var functionRouter = messagingProperties.getFunctionRouter();
        return message -> {
            Optional
                .of(message)
                .filter(it -> it.getHeaders().containsKey(FUNCTION_DESTINATION))
                .map(it -> it.getHeaders().get(FUNCTION_DESTINATION, String.class))
                .map(messagingProperties.getFunctionRouter().getRegistrations()::get)
                .filter(Predicate.not(Collection::isEmpty))
                .ifPresentOrElse(
                    registrations -> {
                        Function<Message<?>, String> resolveFunctionDefinition = functionMessage ->
                            functionMessage.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class);
                        BiFunction<Message<?>, String, Message<?>> toFunctionRequest = (
                                functionMessage,
                                functionRegistration
                            ) ->
                            MessageBuilder
                                .fromMessage(functionMessage)
                                .setHeader(FunctionProperties.FUNCTION_DEFINITION, functionRegistration)
                                .build();

                        var functions = registrations
                            .stream()
                            .map(functionRegistration -> toFunctionRequest.apply(message, functionRegistration))
                            .map(functionRequest ->
                                supplyAsyncWithRetry(
                                        () ->
                                            CompletableFuture.supplyAsync(() -> routingFunction.apply(functionRequest)),
                                        functionRouter.getMaxRetries(),
                                        functionRouter.getRetryInterval()
                                    )
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
                                        log.error(
                                            "Error routing message request {} to function registration {}",
                                            functionRequest,
                                            functionDefinition,
                                            error
                                        );
                                        return Map.entry(functionDefinition, Optional.of(error));
                                    })
                            )
                            .toArray(CompletableFuture[]::new);

                        var completed = CompletableFuture
                            .allOf(functions)
                            .thenApply(v -> Stream.of(functions).map(CompletableFuture::join).toList());

                        completed.thenAccept(results -> {
                            var errors = results
                                .stream()
                                .map(Map.Entry.class::cast)
                                .filter(entry ->
                                    Optional.class.cast(entry.getValue())
                                        .filter(Exception.class::isInstance)
                                        .isPresent()
                                )
                                .map(entry -> Optional.class.cast(entry.getValue()).get())
                                .toList();

                            if (!errors.isEmpty()) {
                                log.debug("Errors handling function route message request {}", errors);
                            } else {
                                log.debug("Successfully completed function route message request {}", message);
                            }
                        });
                    },
                    () -> log.warn("Missing '{}' header to route message {}", FUNCTION_DESTINATION, message)
                );
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
