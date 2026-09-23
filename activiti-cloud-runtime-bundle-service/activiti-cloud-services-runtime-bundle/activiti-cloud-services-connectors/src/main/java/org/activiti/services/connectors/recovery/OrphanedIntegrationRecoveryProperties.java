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
package org.activiti.services.connectors.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Single source of truth for the orphaned-integration-recovery threshold, so it can be shared
 * between {@link OrphanedIntegrationRecoveryScheduler} (which uses it to decide when an integration
 * is orphaned) and {@code IntegrationRequestBuilder} (which copies it into every outgoing
 * {@code IntegrationRequest} as {@code ttlSeconds}) without duplicating the default value.
 */
@ConfigurationProperties("activiti.orphaned-integration-recovery")
public class OrphanedIntegrationRecoveryProperties {

    private int thresholdSeconds = 10800; // 3 hours

    public int getThresholdSeconds() {
        return thresholdSeconds;
    }

    public void setThresholdSeconds(int thresholdSeconds) {
        this.thresholdSeconds = thresholdSeconds;
    }
}
