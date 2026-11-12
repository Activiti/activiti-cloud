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

import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(id = "outbox")
public class OutboxActuatorEndpoint {

    private final OutboxLifecycleController outboxLifecycleController;

    public OutboxActuatorEndpoint(OutboxLifecycleController outboxLifecycleController) {
        this.outboxLifecycleController = outboxLifecycleController;
    }

    /**
     * GET /actuator/outbox
     */
    @ReadOperation
    public Map<String, Boolean> getEndpointsRunningStatus() {
        return outboxLifecycleController.getEndpointsRunningStatus();
    }

    /**
     * POST /actuator/outbox/(start|stop)
     */
    @WriteOperation
    public void smartLifecycleRoleController(@Selector String command) {
        switch (command) {
            case "start":
                outboxLifecycleController.start();
                break;
            case "stop":
                outboxLifecycleController.stop();
                break;
            default:
                throw new IllegalArgumentException("Invalid command argument");
        }
    }
}
