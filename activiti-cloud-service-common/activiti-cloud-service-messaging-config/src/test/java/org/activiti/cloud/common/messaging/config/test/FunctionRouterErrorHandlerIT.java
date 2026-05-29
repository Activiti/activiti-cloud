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
package org.activiti.cloud.common.messaging.config.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration;
import org.activiti.cloud.common.messaging.functional.ConnectorBinding;
import org.activiti.cloud.common.messaging.functional.ConsumerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=foo",
        "spring.application.name=bar",
        "spring.cloud.stream.bindings.restConsumer.destination=rest.GET",
        "spring.cloud.stream.bindings.restConsumer.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.group=${spring.application.name}",
        "activiti.cloud.messaging.function-router.routes.restConsumer.enabled=true",
        "activiti.cloud.messaging.function-router.max-retries=0",
        "activiti.cloud.messaging.function-router.error-handler-definition=functionRouterErrorHandler",
    }
)
@EnableTestBinder
@Import({ TestBindingsChannelsConfiguration.class })
class FunctionRouterErrorHandlerIT {

    private static final AtomicReference<Supplier<RuntimeException>> exceptionSupplier = new AtomicReference<>();
    private static final List<ErrorMessage> capturedErrorMessages = new CopyOnWriteArrayList<>();

    @Autowired
    private BiConsumer<Message<?>, String> functionRouterMessageHandler;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean
        @ConnectorBinding(input = TestBindingsChannels.REST_CONSUMER, connectorType = "rest.GET", condition = "true")
        ConsumerConnector<String> throwingRestConsumer() {
            return payload -> {
                Supplier<RuntimeException> supplier = exceptionSupplier.get();
                if (supplier != null) {
                    throw supplier.get();
                }
            };
        }

        @Bean
        Consumer<ErrorMessage> functionRouterErrorHandler() {
            return capturedErrorMessages::add;
        }
    }

    @AfterEach
    void reset() {
        exceptionSupplier.set(null);
        capturedErrorMessages.clear();
    }

    @Test
    void shouldCallErrorHandlerWithWrappedMessagingExceptionWhenFunctionThrowsRuntimeException() {
        exceptionSupplier.set(() -> new RuntimeException("test error"));

        Message<String> message = MessageBuilder
            .withPayload("test payload")
            .setHeader(FunctionRouterConfiguration.CONNECTOR_TYPE, "rest.GET")
            .build();

        functionRouterMessageHandler.accept(message, FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT);

        assertThat(capturedErrorMessages).hasSize(1);
        Throwable payload = capturedErrorMessages.get(0).getPayload();
        assertThat(payload).isInstanceOf(MessagingException.class);
        assertThat(payload.getCause()).isInstanceOf(RuntimeException.class).hasMessage("test error");
    }

    @Test
    void shouldCallErrorHandlerWithOriginalMessagingExceptionWhenFunctionThrowsMessagingException() {
        Message<String> dummyMessage = MessageBuilder.withPayload("dummy").build();
        MessagingException originalException = new MessagingException(dummyMessage, new RuntimeException("inner error"));
        exceptionSupplier.set(() -> originalException);

        Message<String> message = MessageBuilder
            .withPayload("test payload")
            .setHeader(FunctionRouterConfiguration.CONNECTOR_TYPE, "rest.GET")
            .build();

        functionRouterMessageHandler.accept(message, FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT);

        assertThat(capturedErrorMessages).hasSize(1);
        assertThat(capturedErrorMessages.get(0).getPayload()).isSameAs(originalException);
    }

    @Test
    void shouldCallErrorHandlerAndPreserveCompletionExceptionCauseChainWhenFunctionThrowsCompletionException() {
        RuntimeException innerCause = new RuntimeException("inner cause");
        exceptionSupplier.set(() -> new CompletionException(innerCause));

        Message<String> message = MessageBuilder
            .withPayload("test payload")
            .setHeader(FunctionRouterConfiguration.CONNECTOR_TYPE, "rest.GET")
            .build();

        functionRouterMessageHandler.accept(message, FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT);

        assertThat(capturedErrorMessages).hasSize(1);
        Throwable payload = capturedErrorMessages.get(0).getPayload();
        assertThat(payload).isInstanceOf(MessagingException.class);
        assertThat(payload.getCause()).isInstanceOf(CompletionException.class);
        assertThat(payload.getCause().getCause()).isSameAs(innerCause);
    }
}
