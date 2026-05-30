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

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FunctionRouterExecutorFactory implements Function<String, ExecutorService> {

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private Duration timeout = Duration.ofSeconds(5);
    private int concurrency = 1;
    // Queue capacity controls the number of tasks that can be buffered beyond the pool threads.
    // The default of 1 provides tight backpressure: the submitting thread blocks as soon as
    // all pool threads are busy and one task is already waiting.
    private int queueCapacity = 1;

    private final RejectedExecutionHandler taskExecutionHandler = (runnable, executor) -> {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }

        try {
            // This forces the submitting thread to block and wait
            // until the queue can accept the task.
            // Fix #2: use toMillis()/MILLISECONDS so sub-second timeouts are honoured.
            if (!executor.getQueue().offer(runnable, timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                // Fix #4: human-readable duration in the error message.
                throw new RejectedExecutionException(
                    "Timeout after %dms because queue is full".formatted(timeout.toMillis())
                );
            }
        } catch (InterruptedException e) {
            // Fix #1: restore interrupt flag AND surface the failure to the caller so that
            // the task is not silently dropped.
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting to enqueue task", e);
        }
    };

    private final Function<String, ExecutorService> executorServiceFactory = registration ->
        new ThreadPoolExecutor(
            concurrency,
            concurrency,
            0L,
            TimeUnit.SECONDS,
            // Fix #5: honour configurable queue capacity.
            new LinkedBlockingQueue<>(queueCapacity),
            // Fix #3: append a sequential index so each thread in the pool has a unique name,
            // e.g. "audit-consumer-0", "audit-consumer-1", making thread dumps readable.
            Thread.ofPlatform().name(registration + "-", 0).factory(),
            taskExecutionHandler
        );

    @Override
    public ExecutorService apply(String key) {
        return executors.computeIfAbsent(key, executorServiceFactory);
    }

    @PreDestroy
    public void destroy() {
        try {
            shutdown();

            if (!awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownNow();
        } finally {
            executors.clear();
        }
    }

    public void shutdown() {
        executors.values().forEach(ExecutorService::shutdown);
    }

    public void shutdownNow() {
        executors.values().forEach(ExecutorService::shutdownNow);
    }

    public boolean awaitTermination(final long timeout, TimeUnit timeUnit) throws InterruptedException {
        // Fix #6: use virtual threads for the blocking awaitTermination calls so that the
        // common ForkJoinPool is not polluted with blocking work.
        final var futures = executors
            .values()
            .stream()
            .map(executor -> {
                final var future = new CompletableFuture<Boolean>();
                Thread
                    .ofVirtual()
                    .start(() -> {
                        try {
                            future.complete(executor.awaitTermination(timeout, timeUnit));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            future.complete(false);
                        }
                    });
                return future;
            })
            .toList();

        return CompletableFuture
            .allOf(futures.toArray(CompletableFuture[]::new))
            .thenApply(v -> futures.stream().map(CompletableFuture::join).allMatch(Boolean.TRUE::equals))
            .join();
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
