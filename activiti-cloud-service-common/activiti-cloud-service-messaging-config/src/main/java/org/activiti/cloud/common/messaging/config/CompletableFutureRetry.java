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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompletableFutureRetry {

    private static final Logger log = LoggerFactory.getLogger(CompletableFutureRetry.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static <T> CompletableFuture<T> supplyAsyncWithRetry(
        Supplier<CompletableFuture<T>> supplier,
        int maxRetries
    ) {
        return supplyAsyncWithRetry(supplier, maxRetries, Duration.ZERO);
    }

    public static <T> CompletableFuture<T> supplyAsyncWithRetry(
        Supplier<CompletableFuture<T>> supplier,
        int maxRetries,
        Duration delay
    ) {
        return supplyAsyncWithRetry(supplier, maxRetries, delay, 0);
    }

    private static <T> CompletableFuture<T> supplyAsyncWithRetry(
        Supplier<CompletableFuture<T>> supplier,
        int maxRetries,
        Duration delay,
        int currentAttempt
    ) {
        return supplier
            .get()
            .exceptionallyCompose(exception -> {
                if (currentAttempt < maxRetries) {
                    log.debug("Attempt {} of {} failed. Retrying in {}", currentAttempt + 1, maxRetries, delay);
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> null, scheduler).thenCompose(
                        ignored -> supplyAsyncWithRetry(supplier, maxRetries, delay, currentAttempt + 1)
                    );
                } else {
                    log.debug("Maximum of {} retries reached. Failing operation.", maxRetries, exception);
                    return CompletableFuture.failedFuture(exception);
                }
            });
    }
}
