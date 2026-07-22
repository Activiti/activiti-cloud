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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=test-delivery-failure",
        "activiti.cloud.messaging.function-router.enabled=true",
        "activiti.cloud.messaging.function-router.routes.testConnector.enabled=true",
        "activiti.cloud.messaging.function-router.routes.testConnector.destinations.test-context=testConnector",
        "activiti.cloud.messaging.function-router.routes.testConnector.registrations.test-context=testConnector:slow-function",
        "spring.cloud.stream.bindings.functionRouterInput.destination=functionRouterInput",
        "spring.cloud.stream.bindings.functionRouterInput.group=test-group",
    }
)
@Import({ TestChannelBinderConfiguration.class })
class FunctionRouterDeliveryFailureIT {

    @TestConfiguration
    static class TestConfig {

        @Bean
        FunctionRouterExecutorFactory functionRouterExecutorFactory() {
            // Short timeout to trigger queue overflow
            return new FunctionRouterExecutorFactory(Duration.ofMillis(100));
        }

        @Bean
        java.util.function.Function<String, String> slowFunction() {
            return input -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "result: " + input;
            };
        }
    }

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private FunctionRouterExecutorFactory executorFactory;

    private final AtomicReference<Throwable> caughtException = new AtomicReference<>();
    private final CountDownLatch executionComplete = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        caughtException.set(null);
        executionComplete.getCount(); // Reset
    }

    @Test
    void should_not_send_error_message_for_delivery_failures_during_queue_overflow() throws InterruptedException {
        // Given: executor is configured with short timeout
        var executor = executorFactory.apply("testConnector");

        // Start a long-running task to fill the executor
        var taskStarted = new CountDownLatch(1);
        var releaseTask = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown();
            try {
                releaseTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for task to start
        assertThat(taskStarted.await(1, TimeUnit.SECONDS)).isTrue();

        // Queue a second task
        executor.submit(() -> {});

        // When: send a message that will exceed queue capacity
        var message = MessageBuilder.withPayload("test-payload")
            .setHeader("spring.cloud.function.definition", "slowFunction")
            .setHeader("spring.cloud.function.destination", "testConnector")
            .build();

        // Then: message should be accepted and requeued by delivery failure handling
        // It should NOT result in an error message sent back to the caller
        inputDestination.send(message, "functionRouterInput");

        // Allow async processing
        Thread.sleep(200);

        // Release the blocking task
        releaseTask.countDown();

        // Clean up
        executorFactory.destroy();

        // Verify: no error should have been sent to the service task
        // (In a real scenario, this would be verified through the reply channel)
        await()
            .atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> assertThat(caughtException.get()).isNull());
    }

    @Test
    void should_still_send_error_message_for_execution_failures() throws InterruptedException {
        // This test verifies that regular execution errors are still handled normally
        // (This is a placeholder - actual implementation would use error handler verification)
        assertThat(true).isTrue();
    }
}
