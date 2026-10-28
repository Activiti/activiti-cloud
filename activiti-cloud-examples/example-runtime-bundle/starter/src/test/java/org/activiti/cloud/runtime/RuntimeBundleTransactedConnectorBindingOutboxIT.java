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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.messaging.outbox.OutboxChannelInterceptorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.outbox.enabled=true" })
class RuntimeBundleTransactedConnectorBindingOutboxIT extends RuntimeBundleTransactedConnectorBindingIT {

    @Autowired
    private OutboxChannelInterceptorFactory outboxChannelInterceptorFactory;

    @Autowired
    private HealthIndicator outboxHealthIndicator;

    @Test
    void outboxChannelInterceptorFactory() {
        assertThat(outboxChannelInterceptorFactory.isRunning()).isTrue();
    }

    @Test
    void outboxHealthIndicator() {
        assertThat(outboxHealthIndicator.health()).extracting(Health::getStatus).isEqualTo(Status.UP);
    }
}
