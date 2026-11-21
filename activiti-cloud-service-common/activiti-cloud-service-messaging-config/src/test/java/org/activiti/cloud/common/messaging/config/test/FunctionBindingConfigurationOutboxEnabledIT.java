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
package org.activiti.cloud.common.messaging.config.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.outbox.enabled=true", "spring.sql.init.platform=h2" })
@Import(OutboxTestConfiguration.class)
public class FunctionBindingConfigurationOutboxEnabledIT extends FunctionBindingConfigurationIT {

    @Autowired
    private OutboxTestConfiguration delegate;

    @Test
    void outboxLifecycleController_allEndpointsRunning() {
        delegate.outboxLifecycleController_allEndpointsRunning();
    }

    @Test
    void outboxLifecycleController_getEndpoints() {
        delegate.outboxLifecycleController_getEndpoints();
    }

    @Test
    void outboxLifecycleController_getEndpointsRunningStatus() {
        delegate.outboxLifecycleController_getEndpointsRunningStatus();
    }

    @Test
    void outboxManager_manageEndpoints() {
        delegate.outboxManager_manageEndpoints();
    }
}
