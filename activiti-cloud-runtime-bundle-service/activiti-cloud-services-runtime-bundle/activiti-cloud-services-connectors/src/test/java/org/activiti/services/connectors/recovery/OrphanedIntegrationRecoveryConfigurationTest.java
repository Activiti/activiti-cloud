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

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.services.connectors.channel.IntegrationRequestBuilder;
import org.activiti.services.connectors.channel.ServiceTaskIntegrationErrorEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OrphanedIntegrationRecoveryConfigurationTest {

    private final OrphanedIntegrationRecoveryConfiguration configuration =
        new OrphanedIntegrationRecoveryConfiguration();

    @Mock
    RepositoryService repositoryService;

    @Mock
    IntegrationContextService integrationContextService;

    @Mock
    IntegrationRequestBuilder integrationRequestBuilder;

    @Mock
    ServiceTaskIntegrationErrorEventHandler errorEventHandler;

    @Mock
    FeatureToggle featureToggle;

    @Test
    void should_createConnectorTemplateResolver() {
        var resolver = configuration.connectorTemplateResolver(
            repositoryService,
            JsonMapper.builder().build(),
            new PathMatchingResourcePatternResolver(),
            new OrphanedIntegrationRecoveryProperties()
        );

        assertThat(resolver).isNotNull();
    }

    @Test
    void should_createSchedulerWithConnectorThresholds() {
        var resolver = configuration.connectorTemplateResolver(
            repositoryService,
            JsonMapper.builder().build(),
            new PathMatchingResourcePatternResolver(),
            new OrphanedIntegrationRecoveryProperties()
        );
        var properties = new OrphanedIntegrationRecoveryProperties();
        properties.getConnectorThresholds().put("agenticTaskConnector", -1);

        var scheduler = configuration.orphanedIntegrationRecoveryScheduler(
            integrationContextService,
            integrationRequestBuilder,
            errorEventHandler,
            properties,
            featureToggle,
            resolver
        );

        assertThat(scheduler).isNotNull();
    }
}
