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

import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    value = "activiti.orphaned-integration-recovery.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(OrphanedIntegrationRecoveryProperties.class)
public class OrphanedIntegrationRecoveryConfiguration {

    @Bean
    OrphanedIntegrationRecoveryScheduler orphanedIntegrationRecoveryScheduler(
        IntegrationContextService integrationContextService,
        IntegrationRequestBuilder integrationRequestBuilder,
        ServiceTaskIntegrationErrorEventHandler errorEventHandler,
        OrphanedIntegrationRecoveryProperties properties
    ) {
        return new OrphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            properties
        );
    }
}
