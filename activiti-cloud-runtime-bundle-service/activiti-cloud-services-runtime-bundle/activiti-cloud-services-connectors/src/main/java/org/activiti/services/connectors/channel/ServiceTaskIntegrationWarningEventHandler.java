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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationWarning;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.cmd.integration.DeleteIntegrationContextCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class ServiceTaskIntegrationWarningEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationWarningEventHandler.class);

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ManagementService managementService;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    public ServiceTaskIntegrationWarningEventHandler(
        RuntimeBundleProperties runtimeBundleProperties,
        ManagementService managementService,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.managementService = managementService;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    public void receive(IntegrationWarning integrationWarning) {
        LOGGER.info("Integration warning received: code={}, message={}",
            integrationWarning.getWarningCode(),
            integrationWarning.getWarningMessage());

        // No process stopping. No IntegrationContext deletion. Just log and aggregate.
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            managementService.executeCommand(
                new AggregateIntegrationWarningReceivedEventCmd(
                    integrationWarning,
                    runtimeBundleProperties,
                    processEngineEventsAggregator
                )
            );
        }
    }
}
