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
package org.activiti.services.connectors.channel;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;

public class AggregateIntegrationEventCmdFactory {

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final EphemeralMappingResolver ephemeralMappingResolver;

    public AggregateIntegrationEventCmdFactory(
        RuntimeBundleProperties runtimeBundleProperties,
        ProcessEngineEventsAggregator processEngineEventsAggregator,
        EphemeralMappingResolver ephemeralMappingResolver
    ) {
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
        this.ephemeralMappingResolver = ephemeralMappingResolver;
    }

    AggregateIntegrationResultReceivedEventCmd resultReceived(
        IntegrationContext integrationContext,
        IntegrationContextEntity integrationContextEntity
    ) {
        return new AggregateIntegrationResultReceivedEventCmd(
            integrationContext,
            runtimeBundleProperties,
            processEngineEventsAggregator,
            ephemeralMappingResolver.isEphemeral(integrationContextEntity)
        );
    }

    AggregateIntegrationErrorReceivedEventCmd errorReceived(
        IntegrationError integrationError,
        IntegrationContextEntity integrationContextEntity
    ) {
        return new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator,
            ephemeralMappingResolver.isEphemeral(integrationContextEntity)
        );
    }
}
