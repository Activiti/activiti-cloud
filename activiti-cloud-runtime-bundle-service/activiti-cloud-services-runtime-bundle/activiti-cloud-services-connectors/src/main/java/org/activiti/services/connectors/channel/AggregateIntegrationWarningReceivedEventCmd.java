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
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationWarning;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationWarningReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

class AggregateIntegrationWarningReceivedEventCmd implements Command<Void> {

    private final IntegrationWarning integrationWarning;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    AggregateIntegrationWarningReceivedEventCmd(
        IntegrationWarning integrationWarning,
        RuntimeBundleProperties runtimeBundleProperties,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        this.integrationWarning = integrationWarning;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationWarningReceivedEventImpl warningEvent;
            if (integrationWarning.getIntegrationContext().hasEphemeralVariables()) {
                IntegrationContextImpl sanitizedContext = new IntegrationContextImpl(
                    integrationWarning.getIntegrationContext()
                );
                sanitizedContext.clearOutBoundVariables();
                sanitizedContext.clearInBoundVariables();
                warningEvent = createIntegrationWarningReceivedEvent(sanitizedContext);
            } else {
                warningEvent =
                    createIntegrationWarningReceivedEvent(integrationWarning.getIntegrationContext());
            }
            processEngineEventsAggregator.add(warningEvent);
        }
        return null;
    }

    private CloudIntegrationWarningReceivedEventImpl createIntegrationWarningReceivedEvent(
        IntegrationContext integrationContext
    ) {
        return new CloudIntegrationWarningReceivedEventImpl(
            integrationContext,
            integrationWarning.getWarningCode(),
            integrationWarning.getWarningMessage()
        );
    }
}
