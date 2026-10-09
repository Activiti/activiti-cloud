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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class FunctionRouterExecutorFactory implements Function<String, ExecutorService>, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterExecutorFactory.class);

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private Duration timeout;
    private static final int SINGLE_THREAD_POOL_SIZE = 1;
    private volatile boolean running = false;

    public FunctionRouterExecutorFactory() {
        this(Duration.ofSeconds(300));
    }

    public FunctionRouterExecutorFactory(Duration timeout) {
        this.timeout = timeout;
    }

    private final RejectedExecutionHandler taskExecutionHandler = (runnable, executor) -> {
        if (executor.isShutdown() || executor.isTerminating()) {
            throw new RejectedExecutionException(
                "Executor has been shutdown",
                new IllegalStateException("Executor is shutdown")
            );
        }

        try {
            // This forces the submitting thread to block and wait
            // until the queue can accept the task.
            if (!executor.getQueue().offer(runnable, timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new RejectedExecutionException(
                    "Timeout after %s duration because the queue is full".formatted(timeout),
                    new TimeoutException("Task queue wait timeout")
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RejectedExecutionException("Interrupted while waiting for queue capacity", e);
        }
    };

    private final Function<String, ExecutorService> executorServiceFactory = registration ->
        new ThreadPoolExecutor(
            SINGLE_THREAD_POOL_SIZE,
            SINGLE_THREAD_POOL_SIZE,
            0L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1, true),
            Thread.ofPlatform().name(registration).factory(),
            taskExecutionHandler
        );

    @Override
    public ExecutorService apply(String key) {
        return executors.computeIfAbsent(key, executorServiceFactory);
    }

    public void destroy() {
        try {
            shutdown();

            if (!awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("Workers did not finish within timeout. Forcing hard cancellation...");

                shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted while waiting for tasks to finish.", e);
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
        final var cfs = executors
            .values()
            .stream()
            .map(executor ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return executor.awaitTermination(timeout, timeUnit);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }

                    return false;
                })
            )
            .toList();

        return CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
            .thenApply(v -> cfs.stream().map(CompletableFuture::join).allMatch(Boolean.TRUE::equals))
            .join();
    }

    public Duration getTimeout() {
        return this.timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        destroy();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE + 100;
    }
}
