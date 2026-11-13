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

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.scheduling.TaskScheduler;

public class OutboxLifecycleReconciler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxLifecycleReconciler.class);

    private final OutboxProperties outboxProperties;
    private final ConcurrentMetadataStore outboxMetadataStore;
    private final OutboxLifecycleController outboxLifecycleController;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledTask;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OutboxLifecycleReconciler(
        OutboxProperties outboxProperties,
        ConcurrentMetadataStore outboxMetadataStore,
        OutboxLifecycleController outboxLifecycleController,
        TaskScheduler taskScheduler
    ) {
        this.outboxProperties = outboxProperties;
        this.outboxMetadataStore = outboxMetadataStore;
        this.outboxLifecycleController = outboxLifecycleController;
        this.taskScheduler = taskScheduler;
    }

    public void reconcile() {
        outboxLifecycleController.getEndpointsRunningStatus().forEach((endpoint, isRunning) -> {
            String desiredState = outboxMetadataStore.get(endpoint);
            log.debug(
                "Reconciling Outbox endpoint {} status {} with the desired state {}",
                endpoint,
                isRunning,
                desiredState
            );

            if ("false".equalsIgnoreCase(desiredState) && Boolean.TRUE.equals(isRunning)) {
                outboxLifecycleController.stop(endpoint);
            } else if (Boolean.FALSE.equals(isRunning)) {
                outboxLifecycleController.start(endpoint);
            }
        });
    }

    public void setAutoStartup(boolean status) {
        final var endpoints = outboxLifecycleController.getEndpoints();
        log.warn("{} auto-startup of outbox partitions: {} ", status ? "Enabling" : "Disabling", endpoints);

        endpoints.forEach(key -> outboxMetadataStore.put(key, Boolean.valueOf(status).toString()));
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            Optional.ofNullable(outboxProperties.getAutoStartup()).ifPresent(this::setAutoStartup);

            log.info("Start Outbox reconciler scheduled task with {} period", outboxProperties.getReconcilePeriod());

            this.scheduledTask = taskScheduler.scheduleAtFixedRate(
                this::reconcile,
                Instant.now(),
                outboxProperties.getReconcilePeriod()
            );
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false) && scheduledTask != null) {
            log.info("Stop Outbox reconciler scheduled task");

            // false allows an in-progress task to finish; true interrupts it
            scheduledTask.cancel(false);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
