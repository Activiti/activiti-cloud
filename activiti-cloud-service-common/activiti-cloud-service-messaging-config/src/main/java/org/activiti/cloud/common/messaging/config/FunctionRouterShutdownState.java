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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;

public class FunctionRouterShutdownState implements SmartLifecycle, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(FunctionRouterShutdownState.class);

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<CompletableFuture<?>> inFlight = ConcurrentHashMap.newKeySet();
    private final Duration drainTimeout;

    public FunctionRouterShutdownState(Duration drainTimeout) {
        this.drainTimeout = drainTimeout;
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    public void register(CompletableFuture<?> future) {
        if (future == null) {
            return;
        }
        inFlight.add(future);
        future.whenComplete((result, error) -> inFlight.remove(future));
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown.set(true);
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void stop() {
        shuttingDown.set(true);
        running.set(false);

        var pending = inFlight.toArray(CompletableFuture[]::new);
        if (pending.length == 0) {
            return;
        }

        log.warn(
            "Function router is shutting down; awaiting {} in-flight message(s) to complete (up to {})",
            pending.length,
            drainTimeout
        );
        try {
            CompletableFuture.allOf(pending).get(drainTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.warn("Function router drained all in-flight messages before shutdown");
        } catch (TimeoutException e) {
            log.warn(
                "Function router drain timed out after {}; {} in-flight message(s) did not complete and may be redelivered",
                drainTimeout,
                inFlight.size()
            );
        } catch (ExecutionException e) {
            log.warn("Function router drain completed with errors during shutdown", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Function router drain was interrupted during shutdown");
        }
    }
}
