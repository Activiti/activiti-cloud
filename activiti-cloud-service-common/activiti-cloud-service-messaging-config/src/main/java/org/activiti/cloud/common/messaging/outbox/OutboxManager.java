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

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.jmx.export.annotation.ManagedOperation;

public class OutboxManager {

    private final OutboxLifecycleController outboxLifecycleController;
    private final ConcurrentMetadataStore outboxMetadataStore;
    private final OutboxLifecycleReconciler outboxLifecycleReconciler;

    public OutboxManager(
        OutboxLifecycleController outboxLifecycleController,
        ConcurrentMetadataStore outboxMetadataStore,
        OutboxLifecycleReconciler outboxLifecycleReconciler
    ) {
        this.outboxLifecycleController = outboxLifecycleController;
        this.outboxMetadataStore = outboxMetadataStore;
        this.outboxLifecycleReconciler = outboxLifecycleReconciler;
    }

    @ManagedOperation
    public void start() {
        outboxLifecycleController
            .getEndpoints()
            .forEach(endpoint -> outboxMetadataStore.put(endpoint, Boolean.TRUE.toString()));

        outboxLifecycleReconciler.reconcile();
    }

    @ManagedOperation
    public void stop() {
        outboxLifecycleController
            .getEndpoints()
            .forEach(endpoint -> outboxMetadataStore.put(endpoint, Boolean.FALSE.toString()));

        outboxLifecycleReconciler.reconcile();
    }
}
