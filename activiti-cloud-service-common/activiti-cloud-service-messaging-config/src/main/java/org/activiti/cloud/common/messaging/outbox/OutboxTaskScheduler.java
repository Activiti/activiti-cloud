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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

class OutboxTaskScheduler extends ConcurrentTaskScheduler implements AutoCloseable {

    private final ScheduledExecutorService scheduledExecutor;

    public OutboxTaskScheduler(String partitionKey) {
        this(
            Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name(partitionKey.concat("Poller")).daemon().factory()
            )
        );
    }

    protected OutboxTaskScheduler(ScheduledExecutorService scheduledExecutor) {
        super(scheduledExecutor);
        this.scheduledExecutor = scheduledExecutor;
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutor;
    }

    @Override
    public void close() {
        if (this.scheduledExecutor.isTerminated()) {
            return;
        }

        this.scheduledExecutor.shutdown();
        try {
            if (!this.scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                this.scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException _) {
            this.scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
