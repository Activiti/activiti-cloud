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
package org.activiti.cloud.common.messaging.config.outbox;

import static org.activiti.cloud.common.messaging.config.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;

import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.jmx.export.annotation.ManagedOperation;

public class OutboxManager {

    private final SmartLifecycleRoleController smartLifecycleRoleController;

    public OutboxManager(SmartLifecycleRoleController smartLifecycleRoleController) {
        this.smartLifecycleRoleController = smartLifecycleRoleController;
    }

    @ManagedOperation
    public void start() {
        smartLifecycleRoleController.startLifecyclesInRole(OUTBOX_ROLE);
    }

    @ManagedOperation
    public void stop() {
        smartLifecycleRoleController.stopLifecyclesInRole(OUTBOX_ROLE);
    }
}
