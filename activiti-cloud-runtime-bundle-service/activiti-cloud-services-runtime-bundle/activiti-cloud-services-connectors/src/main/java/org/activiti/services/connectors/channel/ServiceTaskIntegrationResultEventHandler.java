/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.VariablesPropagator;
import org.activiti.engine.impl.cmd.TriggerCmd;
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
import org.springframework.transaction.annotation.Transactional;

public class ServiceTaskIntegrationResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationResultEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ManagementService managementService;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final VariablesPropagator variablesPropagator;

    public ServiceTaskIntegrationResultEventHandler(
        RuntimeService runtimeService,
        IntegrationContextService integrationContextService,
        RuntimeBundleProperties runtimeBundleProperties,
        ManagementService managementService,
        ProcessEngineEventsAggregator processEngineEventsAggregator,
        VariablesPropagator variablesPropagator
    ) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.managementService = managementService;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
        this.variablesPropagator = variablesPropagator;
    }

    @Retryable(
        retryFor = ActivitiOptimisticLockingException.class,
        maxAttemptsExpression = "${activiti.cloud.integration.result.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${activiti.cloud.integration.result.retry.backoff.delay:0}")
    )
    @Transactional(propagation = REQUIRES_NEW)
    public void receive(IntegrationResult integrationResult) {
        IntegrationContext integrationContext = integrationResult.getIntegrationContext();
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(
            integrationContext.getId()
        );

        if (integrationContextEntity == null) {
            return;
        }

        List<Execution> executions = runtimeService
            .createExecutionQuery()
            .executionId(integrationContext.getExecutionId())
            .list();

        if (executions.isEmpty()) {
            LOGGER.warn(
                "No task is in this RB is waiting for integration result with execution id `{}`, flow node id `{}`. " +
                "The integration result for the integration context `{}` will be ignored.",
                integrationContext.getExecutionId(),
                integrationContext.getClientId(),
                integrationContext.getId()
            );
            Command<?> cleanupCmd = getCleanupCmd(integrationContextEntity, integrationContext);
            managementService.executeCommand(cleanupCmd);
            return;
        }

        ExecutionEntity execution = (ExecutionEntity) executions.getFirst();
        if (execution.getActivityId().equals(integrationContext.getClientId())) {
            executeTriggerOrHandleError(integrationResult, execution, integrationContextEntity);
        } else {
            LOGGER.warn(
                "Could not find matching activityId '{}' for integration result '{}' with executionId '{}'",
                integrationContext.getClientId(),
                integrationResult,
                execution.getId()
            );
            // If activity doesn't match, just delete the context and aggregate the event.
            Command<?> cleanupCmd = getCleanupCmd(integrationContextEntity, integrationContext);
            managementService.executeCommand(cleanupCmd);
        }
    }

    private void executeTriggerOrHandleError(
        IntegrationResult integrationResult,
        ExecutionEntity execution,
        IntegrationContextEntity integrationContextEntity
    ) {
        IntegrationContext integrationContext = integrationResult.getIntegrationContext();

        Command<?> triggerAndCleanupCmd = CompositeCommand.of(
            new TriggerCmd(
                integrationContext.getExecutionId(),
                integrationContext.getOutBoundVariables(),
                variablesPropagator
            ),
            new DeleteIntegrationContextCmd(integrationContextEntity),
            new AggregateIntegrationResultReceivedEventCmd(
                integrationContext,
                runtimeBundleProperties,
                processEngineEventsAggregator
            )
        );

        try {
            managementService.executeCommand(triggerAndCleanupCmd);
        } catch (Exception triggerException) {
            LOGGER.error(
                "Failed to trigger execution '{}' with integration result. Attempting to propagate as BPMN error.",
                execution.getId(),
                triggerException
            );
            propagateTriggerFailureWithBPMNErrorPropagation(
                triggerException,
                integrationContext,
                integrationContextEntity,
                execution
            );
        }
    }

    private void propagateTriggerFailureWithBPMNErrorPropagation(
        Exception triggerException,
        IntegrationContext integrationContext,
        IntegrationContextEntity integrationContextEntity,
        ExecutionEntity execution
    ) {
        CloudBpmnError cloudBpmnError = new CloudBpmnError(
            "INTEGRATION_ERROR_RECEIVED",
            triggerException.getMessage(),
            triggerException
        );
        IntegrationErrorImpl integrationError = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            cloudBpmnError
        );

        CompositeCommand bpmnErrorPropagation = CompositeCommand.of(
            new PropagateCloudBpmnErrorCmd(integrationError, execution),
            new DeleteIntegrationContextCmd(integrationContextEntity),
            new AggregateIntegrationErrorReceivedEventCmd(
                integrationError,
                runtimeBundleProperties,
                processEngineEventsAggregator
            )
        );
        managementService.executeCommand(bpmnErrorPropagation);
    }

    private Command<?> getCleanupCmd(
        IntegrationContextEntity integrationContextEntity,
        IntegrationContext integrationContext
    ) {
        // If no execution is found, just delete the context and aggregate the event.
        return CompositeCommand.of(
            new DeleteIntegrationContextCmd(integrationContextEntity),
            new AggregateIntegrationResultReceivedEventCmd(
                integrationContext,
                runtimeBundleProperties,
                processEngineEventsAggregator
            )
        );
    }
}
