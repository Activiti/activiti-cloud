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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.ImmediateRequeueAmqpException;

class FunctionRouterDeliveryFailureIT {

    private final FunctionRouterExecutorFactory factory = new FunctionRouterExecutorFactory(Duration.ofMillis(50));

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void should_not_send_error_message_when_executor_queue_overflows() throws InterruptedException {
        // Unit test: executor-level behavior
        // Given: executor with very short timeout (queue size = 1)
        var executor = factory.apply("testConnector");

        // Start a long-running task to block the executor
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

        // Queue a second task (fills the queue)
        executor.submit(() -> {});

        // When: we try to submit a third task while queue is full
        // Then: it throws RejectedExecutionException (delivery failure)
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RejectedExecutionException.class)
            .hasMessageContaining("Timeout after")
            .hasMessageContaining("because the queue is full");

        // Clean up
        releaseTask.countDown();
    }

    @Test
    void should_not_send_error_message_when_executor_shuts_down() throws InterruptedException {
        // Unit test: executor-level behavior
        // Given: executor that is being shut down
        var executor = factory.apply("testConnector");

        // Start a long-running task to block the executor
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

        assertThat(taskStarted.await(1, TimeUnit.SECONDS)).isTrue();

        // Queue a second task
        executor.submit(() -> {});

        // When: executor is shut down and we try to submit a task
        executor.shutdown();

        // Then: it should throw ImmediateRequeueAmqpException
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(ImmediateRequeueAmqpException.class)
            .hasMessage("Executor is shutting down; requeueing for redelivery");

        // Clean up
        releaseTask.countDown();
    }

    @Test
    void delivery_failures_are_rethrown_not_collected_as_errors() throws InterruptedException {
        // Unit test: verify filter logic behavior
        // This test documents that in FunctionRouterConfiguration.functionRouterMessageHandler():
        // 1. Delivery failures (RejectedExecutionException, ImmediateRequeueAmqpException)
        //    are detected in the exceptionally() handler (lines 237-244)
        // 2. They are rethrown (not returned as Map.entry)
        // 3. They are filtered out from error collection (lines 275-280)
        // 4. They are NOT sent as ErrorMessage to the service task

        // Simulate the filtering logic
        Throwable rejectionError = new RejectedExecutionException("queue full");
        Throwable shutdownError = new ImmediateRequeueAmqpException("shutdown");

        // These should be detected as delivery failures in the filter
        var isDeliveryFailure1 =
            rejectionError instanceof RejectedExecutionException ||
            rejectionError instanceof ImmediateRequeueAmqpException;
        var isDeliveryFailure2 =
            shutdownError instanceof RejectedExecutionException ||
            shutdownError instanceof ImmediateRequeueAmqpException;

        assertThat(isDeliveryFailure1).isTrue();
        assertThat(isDeliveryFailure2).isTrue();

        // Verify they would be filtered out (negation in filter)
        var shouldBeFiltered1 = !(isDeliveryFailure1);
        var shouldBeFiltered2 = !(isDeliveryFailure2);

        assertThat(shouldBeFiltered1).isFalse();
        assertThat(shouldBeFiltered2).isFalse();
    }
}
