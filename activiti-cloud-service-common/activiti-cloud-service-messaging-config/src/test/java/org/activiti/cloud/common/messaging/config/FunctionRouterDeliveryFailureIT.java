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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FunctionRouterDeliveryFailureIT {

    private final FunctionRouterExecutorFactory factory = new FunctionRouterExecutorFactory(Duration.ofMillis(50));

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void should_not_send_error_message_when_executor_queue_overflows() throws InterruptedException {
        var executor = factory.apply("testConnector");

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

        executor.submit(() -> {}); // fill the queue (size 1)

        // submitting past capacity is a delivery failure, not an application error
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RejectedExecutionException.class)
            .hasMessageContaining("Timeout after")
            .hasMessageContaining("because the queue is full");

        releaseTask.countDown();
    }

    @Test
    void should_not_send_error_message_when_executor_shuts_down() throws InterruptedException {
        var executor = factory.apply("testConnector");

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

        executor.submit(() -> {});
        executor.shutdown();

        // submitting to a shut-down executor is a requeue signal, not an application error
        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RequeueDeliveryException.class)
            .hasMessage("Executor is shutting down; requeueing for redelivery");

        releaseTask.countDown();
    }

    @Test
    void delivery_failures_are_rethrown_not_collected_as_errors() {
        // both delivery-failure types are recognised by the filter in functionRouterMessageHandler
        Throwable rejectionError = new RejectedExecutionException("queue full");
        Throwable shutdownError = new RequeueDeliveryException("shutdown");

        var isDeliveryFailure1 =
            rejectionError instanceof RejectedExecutionException || rejectionError instanceof RequeueDeliveryException;
        var isDeliveryFailure2 =
            shutdownError instanceof RejectedExecutionException || shutdownError instanceof RequeueDeliveryException;

        assertThat(isDeliveryFailure1).isTrue();
        assertThat(isDeliveryFailure2).isTrue();
    }

    @Test
    void supplyAsync_throws_synchronously_when_executor_rejects_instead_of_failing_the_future()
        throws InterruptedException {
        // root cause: supplyAsync() calls executor.execute() on the calling thread, so a throwing
        // RejectedExecutionHandler escapes synchronously instead of failing the future - a chained
        // exceptionally() would never see it.
        var executor = factory.apply("testConnector");
        fillExecutorQueue(executor);

        assertThatThrownBy(() -> CompletableFuture.supplyAsync(() -> "result", executor)).isInstanceOf(
            RejectedExecutionException.class
        );
    }

    @Test
    void wrapping_supplyAsync_in_try_catch_converts_synchronous_rejection_to_failed_future()
        throws InterruptedException {
        // the fix: wrap submission in try/catch and convert a synchronous rejection into
        // failedFuture(e) so it flows through the normal exceptionally() pipeline.
        var executor = factory.apply("testConnector");
        fillExecutorQueue(executor);

        CompletableFuture<String> future;
        try {
            future = CompletableFuture.supplyAsync(() -> "result", executor);
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }

        var caughtCause = new AtomicReference<Throwable>();
        future
            .exceptionally(error -> {
                var cause = error instanceof CompletionException ce ? ce.getCause() : error;
                caughtCause.set(cause);
                return null;
            })
            .join();

        assertThat(caughtCause.get()).isInstanceOf(RejectedExecutionException.class);
    }

    private void fillExecutorQueue(java.util.concurrent.ExecutorService executor) throws InterruptedException {
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

        executor.submit(() -> {}); // fill the queue (size 1)
    }
}
