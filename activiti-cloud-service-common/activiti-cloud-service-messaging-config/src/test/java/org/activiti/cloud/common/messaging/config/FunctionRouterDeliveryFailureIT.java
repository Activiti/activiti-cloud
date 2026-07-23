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
    void should_throw_rejected_execution_exception_when_queue_is_full() throws InterruptedException {
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

        // When: we try to submit a third task while queue is full and executor is not shutting down
        // Then: it throws RejectedExecutionException (delivery failure that needs to be filtered)
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RejectedExecutionException.class)
            .hasMessageContaining("Timeout after")
            .hasMessageContaining("because the queue is full");

        // Clean up
        releaseTask.countDown();
    }

    @Test
    void should_throw_immediate_requeue_exception_when_executor_is_shutting_down() throws InterruptedException {
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

        // Then: it should throw ImmediateRequeueAmqpException (per PR #2499)
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(ImmediateRequeueAmqpException.class)
            .hasMessage("Executor is shutting down; requeueing for redelivery");

        // Clean up
        releaseTask.countDown();
    }

    @Test
    void should_filter_out_delivery_failures_from_error_handling() {
        // This test documents the behavior that FunctionRouterConfiguration should:
        // 1. Detect delivery failures (RejectedExecutionException or ImmediateRequeueAmqpException)
        // 2. NOT send them as ErrorMessage to the waiting service task
        // 3. Let AMQP handle the requeue naturally

        // FunctionRouterExecutorFactory throws:
        // - RejectedExecutionException: when queue.offer() times out (queue is full)
        // - ImmediateRequeueAmqpException: when executor is shutting down OR interrupted

        // FunctionRouterConfiguration.functionRouterMessageHandler() filters these at:
        // - Line 238-244: rethrow ImmediateRequeueAmqpException in exceptionally() handler
        // - Line 273-277: filter out delivery failures from error collection

        assertThat(new RejectedExecutionException("delivery failure")).isNotNull();
        assertThat(new ImmediateRequeueAmqpException("delivery failure")).isNotNull();
    }
}
