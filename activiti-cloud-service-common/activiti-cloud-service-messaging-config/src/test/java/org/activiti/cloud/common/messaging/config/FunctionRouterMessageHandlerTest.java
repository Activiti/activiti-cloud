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

import static org.activiti.cloud.common.messaging.config.FunctionRouterConfiguration.FUNCTION_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class FunctionRouterMessageHandlerTest {

    private static final String ROUTING_CONTEXT = "test";
    private static final String BINDING_NAME = "integrationRequests";
    private static final String DESTINATION = "integration-requests";
    private static final String TIMING_OUT_FUNCTION = "timingOutHandler";
    private static final String FAILING_FUNCTION = "failingHandler";

    @Mock
    private RoutingFunction routingFunction;

    @Mock
    private ExecutorService timingOutExecutor;

    @Mock
    private ExecutorService failingExecutor;

    @Mock
    private Future<Object> timingOutTask;

    @Mock
    private Future<Object> failingTask;

    @Mock
    private Consumer<ErrorMessage> bindingErrorHandler;

    private ActivitiCloudMessagingProperties messagingProperties;

    private FunctionRouterMessageHandler handler;

    private final Message<String> message = MessageBuilder.withPayload("Test")
        .setHeader(FUNCTION_DESTINATION, DESTINATION)
        .build();

    @BeforeEach
    void setUp() {
        messagingProperties = new ActivitiCloudMessagingProperties();
        messagingProperties.getFunctionRouter().destinations(ROUTING_CONTEXT).put(BINDING_NAME, DESTINATION);

        final Map<String, ExecutorService> executors = Map.of(
            TIMING_OUT_FUNCTION,
            timingOutExecutor,
            FAILING_FUNCTION,
            failingExecutor
        );

        handler = new FunctionRouterMessageHandler(
            routingFunction,
            messagingProperties,
            request -> executors.get(request.getHeaders().get(FunctionProperties.FUNCTION_DEFINITION, String.class)),
            new MessageContentTypeNormalizer(),
            new BindingServiceProperties(),
            bindingName -> Optional.of(bindingErrorHandler)
        );
    }

    @Test
    @DisplayName("Should surface timeout without invoking binding error handler when per-function timeout fires first")
    void shouldSurfaceTimeoutWhenPerFunctionTimeoutFiresFirst() throws Exception {
        // given
        messagingProperties.getFunctionRouter().setRequestTimeout(Duration.ofSeconds(30));
        register(TIMING_OUT_FUNCTION);
        doReturn(timingOutTask).when(timingOutExecutor).submit(any(Callable.class));
        doThrow(new TimeoutException("per-function timeout")).when(timingOutTask).get(anyLong(), any(TimeUnit.class));

        // when
        assertThatThrownBy(() -> handler.accept(message, ROUTING_CONTEXT))
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(TimeoutException.class)
            .hasRootCauseMessage("per-function timeout");

        // then
        verify(timingOutTask).cancel(true);
        verify(bindingErrorHandler, never()).accept(any(ErrorMessage.class));
    }

    @Test
    @DisplayName("Should surface timeout without invoking binding error handler when request timeout fires first")
    void shouldSurfaceTimeoutWhenRequestTimeoutFiresFirst() throws Exception {
        // given
        messagingProperties.getFunctionRouter().setRequestTimeout(Duration.ofMillis(100));
        register(TIMING_OUT_FUNCTION);
        final var releasePerFunctionTimeout = new CountDownLatch(1);
        doReturn(timingOutTask).when(timingOutExecutor).submit(any(Callable.class));
        doAnswer(invocation -> {
            releasePerFunctionTimeout.await();
            throw new TimeoutException("per-function timeout");
        })
            .when(timingOutTask)
            .get(anyLong(), any(TimeUnit.class));

        // when
        try {
            assertThatThrownBy(() -> handler.accept(message, ROUTING_CONTEXT))
                .isInstanceOf(CompletionException.class)
                .hasRootCauseInstanceOf(TimeoutException.class)
                .satisfies(error -> assertThat(error.getCause().getMessage()).isNotEqualTo("per-function timeout"));
        } finally {
            releasePerFunctionTimeout.countDown();
        }

        // then
        verify(timingOutTask, timeout(1000)).cancel(true);
        verify(bindingErrorHandler, after(200).never()).accept(any(ErrorMessage.class));
    }

    @Test
    @DisplayName("Should surface timeout and skip binding error handlers of other failed functions")
    void shouldSurfaceTimeoutWhenOtherFunctionFails() throws Exception {
        // given
        messagingProperties.getFunctionRouter().setRequestTimeout(Duration.ofSeconds(30));
        register(TIMING_OUT_FUNCTION);
        register(FAILING_FUNCTION);
        doReturn(timingOutTask).when(timingOutExecutor).submit(any(Callable.class));
        doThrow(new TimeoutException("per-function timeout")).when(timingOutTask).get(anyLong(), any(TimeUnit.class));
        doReturn(failingTask).when(failingExecutor).submit(any(Callable.class));
        doReturn(CompletableFuture.failedFuture(new RuntimeException("function error")))
            .when(failingTask)
            .get(anyLong(), any(TimeUnit.class));

        // when
        assertThatThrownBy(() -> handler.accept(message, ROUTING_CONTEXT))
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(TimeoutException.class);

        // then
        verify(bindingErrorHandler, never()).accept(any(ErrorMessage.class));
    }

    @Test
    @DisplayName("Should invoke binding error handler when function fails without timeout")
    void shouldInvokeBindingErrorHandlerWhenFunctionFails() throws Exception {
        // given
        messagingProperties.getFunctionRouter().setRequestTimeout(Duration.ofSeconds(30));
        register(FAILING_FUNCTION);
        doReturn(failingTask).when(failingExecutor).submit(any(Callable.class));
        doReturn(CompletableFuture.failedFuture(new RuntimeException("function error")))
            .when(failingTask)
            .get(anyLong(), any(TimeUnit.class));

        // when
        handler.accept(message, ROUTING_CONTEXT);

        // then
        verify(bindingErrorHandler).accept(any(ErrorMessage.class));
    }

    private void register(String functionName) {
        messagingProperties.getFunctionRouter().register(BINDING_NAME, functionName);
    }
}
