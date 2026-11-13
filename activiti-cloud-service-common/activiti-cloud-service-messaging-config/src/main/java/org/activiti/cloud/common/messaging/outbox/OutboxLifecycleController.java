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

import static org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory.OUTBOX_ROLE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

public class OutboxLifecycleController {

    private final SmartLifecycleRoleController smartLifecycleRoleController;
    private final ApplicationContext applicationContext;

    public OutboxLifecycleController(
        SmartLifecycleRoleController smartLifecycleRoleController,
        ApplicationContext applicationContext
    ) {
        this.smartLifecycleRoleController = smartLifecycleRoleController;
        this.applicationContext = applicationContext;
    }

    @ManagedOperation
    public void start() {
        smartLifecycleRoleController.startLifecyclesInRole(OUTBOX_ROLE);
    }

    @ManagedOperation
    public void stop() {
        smartLifecycleRoleController.stopLifecyclesInRole(OUTBOX_ROLE);
    }

    @ManagedAttribute
    public Map<String, Boolean> getEndpointsRunningStatus() {
        return smartLifecycleRoleController.getEndpointsRunningStatus(OUTBOX_ROLE);
    }

    @ManagedAttribute
    public boolean allEndpointsRunning() {
        return smartLifecycleRoleController.allEndpointsRunning(OUTBOX_ROLE);
    }

    @ManagedAttribute
    public List<String> getEndpoints() {
        return smartLifecycleRoleController.getEndpointsRunningStatus(OUTBOX_ROLE).keySet().stream().toList();
    }

    @ManagedOperation
    public void start(String endpoint) {
        smartLifecycle(endpoint).ifPresent(SmartLifecycle::start);
    }

    @ManagedOperation
    public void stop(String endpoint) {
        smartLifecycle(endpoint).ifPresent(SmartLifecycle::stop);
    }

    private Optional<SmartLifecycle> smartLifecycle(String endpoint) {
        return Optional.ofNullable(endpoint)
            .filter(applicationContext::containsBean)
            .map(_ -> applicationContext.getBean(endpoint, SmartLifecycle.class));
    }
}
