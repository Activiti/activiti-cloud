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
import java.util.Map;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.VariablesPropagator;
import org.activiti.engine.impl.cmd.SetExecutionVariablesCmd;
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

        if (integrationContextEntity != null) {
            List<Command<?>> commands = new ArrayList<>();
            String executionId = integrationContext.getExecutionId();
            List<Execution> executions = runtimeService.createExecutionQuery().executionId(executionId).list();
            if (!executions.isEmpty()) {
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
                        executions.get(0).getId()
                    );
                }
            } else {
                LOGGER.warn(
                    "No task is waiting for integration result with execution id `{}`, flow node id `{}`. " +
                    "Integration context `{}` result will be ignored.",
                    executionId,
                    integrationContext.getClientId(),
                    integrationContext.getId()
                );
            }
            commands.add(new DeleteIntegrationContextCmd(integrationContextEntity));
            commands.add(
                new AggregateIntegrationResultReceivedEventCmd(
                    integrationContext,
                    runtimeBundleProperties,
                    processEngineEventsAggregator
                )
            );

            try {
                managementService.executeCommand(CompositeCommand.of(commands.toArray(Command[]::new)));
            } catch (Exception triggerException) {
                String message =
                    "Error processing integration result for integration context " + integrationContext.getId();
                LOGGER.error(message, triggerException);

                // Requery execution because state may have changed
                Execution execAfterFailure = runtimeService
                    .createExecutionQuery()
                    .executionId(executionId)
                    .singleResult();

                IntegrationRequest fakeRequest = new IntegrationRequestImpl(integrationContext);
                IntegrationErrorImpl integrationError = new IntegrationErrorImpl(fakeRequest, triggerException);

                List<Command<?>> errorCommands = new ArrayList<>();

                if (execAfterFailure != null) {
                    // mark failure (local + process variables)
                    errorCommands.add(
                        new SetExecutionVariablesCmd(
                            execAfterFailure.getId(),
                            Map.of(
                                "integrationFailure",
                                true,
                                "failureActivityId",
                                execAfterFailure.getActivityId(),
                                "failureMessage",
                                integrationError.getErrorMessage()
                            ),
                            true
                        )
                    );
                    errorCommands.add(
                        new SetExecutionVariablesCmd(
                            execAfterFailure.getProcessInstanceId(),
                            Map.of("processStatus", "FAILED", "lastFailureTime", System.currentTimeMillis()),
                            false
                        )
                    );
                } else {
                    LOGGER.warn("Skipping failure variable recording: execution `{}` no longer exists.", executionId);
                }

                errorCommands.add(
                    new AggregateIntegrationErrorReceivedEventCmd(
                        integrationError,
                        runtimeBundleProperties,
                        processEngineEventsAggregator
                    )
                );
                errorCommands.add(new DeleteIntegrationContextCmd(integrationContextEntity));

                managementService.executeCommand(CompositeCommand.of(errorCommands.toArray(Command[]::new)));
            }
        }
    }
}
