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

package org.activiti.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DisposableConcurrentTaskSchedulerTest {

    @Test
    void shouldScheduleTasksAndShutdownExecutorCleanlyOnDestroy() throws Exception {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        DisposableConcurrentTaskScheduler scheduler = new DisposableConcurrentTaskScheduler(executor);
        CountDownLatch taskExecuted = new CountDownLatch(1);

        scheduler.schedule(taskExecuted::countDown, Instant.now());

        assertThat(taskExecuted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.isShutdown()).isFalse();

        scheduler.destroy();

        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    void shouldDoNothingWhenExecutorIsAlreadyTerminated() throws Exception {
        AtomicInteger shutdownCalls = new AtomicInteger();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1) {
            @Override
            public void shutdown() {
                shutdownCalls.incrementAndGet();
                super.shutdown();
            }
        };
        executor.shutdown();
        assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        assertThat(shutdownCalls).hasValue(1);

        DisposableConcurrentTaskScheduler scheduler = new DisposableConcurrentTaskScheduler(executor);
        scheduler.destroy();

        assertThat(shutdownCalls).hasValue(1);
        assertThat(executor.isTerminated()).isTrue();
    }
}
