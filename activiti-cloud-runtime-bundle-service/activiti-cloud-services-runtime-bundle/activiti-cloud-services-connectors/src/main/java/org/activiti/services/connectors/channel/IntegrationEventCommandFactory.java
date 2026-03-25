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

public class IntegrationEventCommandFactory {

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    public IntegrationEventCommandFactory(
        RuntimeBundleProperties runtimeBundleProperties,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    AggregateIntegrationResultReceivedEventCmd createResultReceivedEventCmd(IntegrationContext integrationContext) {
        return new AggregateIntegrationResultReceivedEventCmd(
            integrationContext,
            runtimeBundleProperties,
            processEngineEventsAggregator
        );
    }

    AggregateIntegrationErrorReceivedEventCmd createErrorReceivedEventCmd(IntegrationError integrationError) {
        return new AggregateIntegrationErrorReceivedEventCmd(
            integrationError,
            runtimeBundleProperties,
            processEngineEventsAggregator
        );
    }
}
