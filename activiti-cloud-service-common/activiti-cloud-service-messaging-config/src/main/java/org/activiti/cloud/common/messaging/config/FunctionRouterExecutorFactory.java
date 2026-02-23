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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FunctionRouterExecutorFactory implements Function<String, ExecutorService> {

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private Duration timeout = Duration.ofSeconds(5);

    private final Function<String, ExecutorService> executorServiceFactory = registration ->
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            final var thread = new Thread(runnable);
            thread.setName(registration);

            return thread;
        });

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

        return CompletableFuture
            .allOf(cfs.toArray(CompletableFuture[]::new))
            .thenApply(v -> cfs.stream().map(CompletableFuture::join).allMatch(Boolean.TRUE::equals))
            .join();
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
