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

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.VariablesPropagator;
import org.activiti.engine.impl.cmd.TriggerCmd;
import org.activiti.engine.impl.cmd.integration.DeleteIntegrationContextCmd;
import org.activiti.engine.impl.interceptor.Command;
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
        value = ActivitiOptimisticLockingException.class,
        maxAttemptsExpression = "${activiti.cloud.integration.result.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${activiti.cloud.integration.result.retry.backoff.delay:0}")
    )
    @Transactional(propagation = REQUIRES_NEW)
    public void receive(IntegrationResult integrationResult) {
        IntegrationContext integrationContext = integrationResult.getIntegrationContext();
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(
            integrationContext.getId()
        );

        if (integrationContextEntity != null) {
            List<Command<?>> commands = new ArrayList<>();

            commands.add(new DeleteIntegrationContextCmd(integrationContextEntity));

            String executionId = integrationContext.getExecutionId();
            List<Execution> executions = runtimeService.createExecutionQuery().executionId(executionId).list();
            if (executions.size() > 0) {
                Execution execution = executions.get(0);

                if (execution.getActivityId().equals(integrationContext.getClientId())) {
                    commands.add(
                        new TriggerCmd(
                            integrationContext.getExecutionId(),
                            integrationContext.getOutBoundVariables(),
                            variablesPropagator
                        )
                    );
                } else {
                    LOGGER.warn(
                        "Could not find matching activityId '{}' for integration result '{}' with executionId '{}'",
                        integrationContext.getClientId(),
                        integrationResult,
                        execution.getId()
                    );
                }
            } else {
                String message =
                    "No task is in this RB is waiting for integration result with execution id `" +
                    executionId +
                    ", flow node id `" +
                    integrationContext.getClientId() +
                    "`. The integration result for the integration context `" +
                    integrationContext.getId() +
                    "` will be ignored.";
                LOGGER.warn(message);
            }

            commands.add(
                new AggregateIntegrationResultReceivedEventCmd(
                    integrationContext,
                    runtimeBundleProperties,
                    processEngineEventsAggregator
                )
            );

            managementService.executeCommand(CompositeCommand.of(commands.toArray(Command[]::new)));
        }
    }
}
