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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionRouterExecutorFactory implements Function<String, ExecutorService> {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterExecutorFactory.class);

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private Duration timeout = Duration.ofSeconds(5);
    private static final int SINGLE_THREAD_POOL_SIZE = 1;
    private final boolean useVirtualThreads;
    private final int maxConcurrency;

    public FunctionRouterExecutorFactory() {
        this(Duration.ofSeconds(5), false, SINGLE_THREAD_POOL_SIZE);
    }

    public FunctionRouterExecutorFactory(Duration timeout) {
        this(timeout, false, SINGLE_THREAD_POOL_SIZE);
    }

    public FunctionRouterExecutorFactory(Duration timeout, boolean useVirtualThreads, int maxConcurrency) {
        this.timeout = timeout;
        this.useVirtualThreads = useVirtualThreads;
        this.maxConcurrency = maxConcurrency;
        if (useVirtualThreads) {
            log.info("Virtual threads enabled for function router executors with max concurrency {}", maxConcurrency);
        }
    }

    private final RejectedExecutionHandler taskExecutionHandler = (runnable, executor) -> {
        if (executor.isShutdown() || executor.isTerminating()) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }

        try {
            // This forces the submitting thread to block and wait
            // until the queue can accept the task.
            if (!executor.getQueue().offer(runnable, timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new RejectedExecutionException(
                    "Timeout after %s duration because the queue is full".formatted(timeout)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RejectedExecutionException("Interrupted while waiting for queue capacity", e);
        }
    };

    private ExecutorService createExecutor(String registration) {
        if (useVirtualThreads) {
            ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(registration + "-vt-", 0).factory()
            );
            if (maxConcurrency > 0) {
                return new SemaphoreBoundExecutor(virtualExecutor, maxConcurrency, timeout);
            }
            return virtualExecutor;
        }
        return new ThreadPoolExecutor(
            SINGLE_THREAD_POOL_SIZE,
            SINGLE_THREAD_POOL_SIZE,
            0L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1, true),
            Thread.ofPlatform().name(registration).factory(),
            taskExecutionHandler
        );
    }

    @Override
    public ExecutorService apply(String key) {
        return executors.computeIfAbsent(key, this::createExecutor);
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

    /**
     * Wraps a virtual-thread-per-task executor with a semaphore to cap the number of
     * concurrently running tasks per registration, preserving backpressure semantics.
     */
    private static class SemaphoreBoundExecutor implements ExecutorService {

        private final ExecutorService delegate;
        private final Semaphore semaphore;
        private final Duration acquireTimeout;

        SemaphoreBoundExecutor(ExecutorService delegate, int maxConcurrency, Duration acquireTimeout) {
            this.delegate = delegate;
            this.semaphore = new Semaphore(maxConcurrency, true);
            this.acquireTimeout = acquireTimeout;
        }

        @Override
        public void execute(Runnable command) {
            try {
                if (!semaphore.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new RejectedExecutionException(
                        "Timeout after %s waiting for virtual thread concurrency permit".formatted(acquireTimeout)
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while waiting for concurrency permit", e);
            }
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            });
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }
}
