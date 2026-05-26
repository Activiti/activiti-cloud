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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
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
        "activiti.cloud.messaging.function-router.processing-timeout=1s",
    }
)
@EnableTestBinder
@Import({ TestBindingsChannelsConfiguration.class })
class FunctionRouterProcessingTimeoutIT {

    private static final AtomicReference<CountDownLatch> handlerStartedLatch = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch> blockingLatch = new AtomicReference<>();

    @Autowired
    private BiConsumer<Message<?>, String> functionRouterMessageHandler;

    @TestConfiguration
    static class ApplicationConfig {

        @Bean
        @ConnectorBinding(input = TestBindingsChannels.REST_CONSUMER, connectorType = "rest.GET", condition = "true")
        ConsumerConnector<String> restGetHandler() {
            return payload -> {
                CountDownLatch started = handlerStartedLatch.get();
                if (started != null) {
                    started.countDown();
                }
                CountDownLatch latch = blockingLatch.get();
                if (latch != null) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };
        }
    }

    @AfterEach
    void tearDown() {
        CountDownLatch started = handlerStartedLatch.getAndSet(null);
        if (started != null) {
            while (started.getCount() > 0) {
                started.countDown();
            }
        }
        CountDownLatch latch = blockingLatch.getAndSet(null);
        if (latch != null) {
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }
    }

    @Test
    void shouldUnblockCallerAfterProcessingTimeoutExpires() {
        final CountDownLatch neverReleasedLatch = new CountDownLatch(1);
        blockingLatch.set(neverReleasedLatch);

        Message<String> message = MessageBuilder
            .withPayload("GET http://localhost:8080")
            .setHeader(FunctionRouterConfiguration.CONNECTOR_TYPE, "rest.GET")
            .build();

        CompletableFuture<Void> senderFuture = CompletableFuture.runAsync(() -> {
            try {
                functionRouterMessageHandler.accept(message, FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT);
            } catch (Exception ignored) {}
        });

        assertThat(senderFuture).succeedsWithin(Duration.ofSeconds(10));
        assertThat(neverReleasedLatch.getCount())
            .as("latch must not have been released by the router — unblocking was caused by the timeout")
            .isEqualTo(1);
    }

    @Test
    void shouldThrowMessagingExceptionAndPreserveInterruptFlagWhenCallerIsInterrupted() throws InterruptedException {
        handlerStartedLatch.set(new CountDownLatch(1));
        blockingLatch.set(new CountDownLatch(1));

        Message<String> message = MessageBuilder
            .withPayload("test")
            .setHeader(FunctionRouterConfiguration.CONNECTOR_TYPE, "rest.GET")
            .build();

        AtomicReference<Throwable> thrownException = new AtomicReference<>();
        AtomicBoolean interruptFlagPreserved = new AtomicBoolean(false);

        Thread callerThread = new Thread(() -> {
            try {
                functionRouterMessageHandler.accept(message, FunctionRouterConfiguration.FUNCTION_ROUTER_INPUT);
            } catch (MessagingException e) {
                thrownException.set(e);
                interruptFlagPreserved.set(Thread.currentThread().isInterrupted());
            }
        });
        callerThread.start();

        assertThat(handlerStartedLatch.get().await(5, TimeUnit.SECONDS))
            .as("handler must start within 5s")
            .isTrue();
        callerThread.interrupt();
        callerThread.join(5_000);

        assertThat(thrownException.get())
            .isInstanceOf(MessagingException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(InterruptedException.class);
        assertThat(interruptFlagPreserved)
            .as("interrupt flag must be re-set on the caller thread after InterruptedException is caught")
            .isTrue();
    }
}
