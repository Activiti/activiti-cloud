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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FunctionRouterExecutorFactoryTest {

    private final FunctionRouterExecutorFactory factory = new FunctionRouterExecutorFactory();

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void shouldReuseExecutorsPerRegistration() {
        final var firstExecutor = factory.apply("foo-registration");
        final var sameRegistrationExecutor = factory.apply("foo-registration");
        final var differentRegistrationExecutor = factory.apply("bar-registration");

        assertThat(firstExecutor).isSameAs(sameRegistrationExecutor);
        assertThat(differentRegistrationExecutor).isNotSameAs(firstExecutor);
    }

    @Test
    void shouldRejectTasksAfterShutdown() {
        final var executor = factory.apply("foo-registration");

        executor.shutdown();

        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RejectedExecutionException.class)
            .hasMessage("Executor has been shutdown");
    }

    @Test
    void shouldRejectTasksWhenQueueRemainsFullUntilTimeout() throws InterruptedException {
        factory.setTimeout(Duration.ofMillis(20));

        final var executor = factory.apply("foo-registration");
        final var taskStarted = new CountDownLatch(1);
        final var releaseTask = new CountDownLatch(1);
        final var queuedTaskExecuted = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown();
            await(releaseTask);
        });

        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        executor.submit(queuedTaskExecuted::countDown);

        assertThatThrownBy(() -> executor.submit(() -> {}))
            .isInstanceOf(RejectedExecutionException.class)
            .hasMessageContaining("queue is full");

        releaseTask.countDown();

        assertThat(queuedTaskExecuted.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void shouldRestoreInterruptStatusWhenInterruptedWhileWaitingForQueueCapacity() throws InterruptedException {
        factory.setTimeout(Duration.ofSeconds(1));

        final var executor = factory.apply("foo-registration");
        final var taskStarted = new CountDownLatch(1);
        final var releaseTask = new CountDownLatch(1);
        final var interrupted = new AtomicBoolean();
        final var thrown = new AtomicReference<Throwable>();

        executor.submit(() -> {
            taskStarted.countDown();
            await(releaseTask);
        });

        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        executor.submit(() -> {});

        final var submitter = Thread.ofPlatform().start(() -> {
            try {
                Thread.currentThread().interrupt();
                executor.submit(() -> {});
                interrupted.set(Thread.currentThread().isInterrupted());
            } catch (Throwable throwable) {
                thrown.set(throwable);
            }
        });

        submitter.join(TimeUnit.SECONDS.toMillis(5));
        releaseTask.countDown();

        assertThat(submitter.isAlive()).isFalse();
        assertThat(thrown.get()).isNull();
        assertThat(interrupted.get()).isTrue();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
