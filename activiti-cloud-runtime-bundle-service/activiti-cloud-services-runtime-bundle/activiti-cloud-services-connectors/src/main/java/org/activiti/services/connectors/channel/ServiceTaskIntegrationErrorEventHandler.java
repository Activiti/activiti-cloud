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

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
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

public class ServiceTaskIntegrationErrorEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationErrorEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ManagementService managementService;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    public ServiceTaskIntegrationErrorEventHandler(
        RuntimeService runtimeService,
        IntegrationContextService integrationContextService,
        ManagementService managementService,
        RuntimeBundleProperties runtimeBundleProperties,
        ProcessEngineEventsAggregator processEngineEventsAggregator
    ) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.managementService = managementService;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    @Retryable(
        value = ActivitiOptimisticLockingException.class,
        maxAttemptsExpression = "${activiti.cloud.integration.error.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${activiti.cloud.integration.error.retry.backoff.delay:0}")
    )
    public void receive(IntegrationError integrationError) {
        IntegrationContext integrationContext = integrationError.getIntegrationContext();
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(
            integrationContext.getId()
        );

        if (integrationContextEntity != null) {
            List<Command<?>> commands = new ArrayList<>();

            commands.add(new DeleteIntegrationContextCmd(integrationContextEntity));

            List<Execution> executions = runtimeService
                .createExecutionQuery()
                .executionId(integrationContextEntity.getExecutionId())
                .list();
            if (executions.size() > 0) {
                ExecutionEntity execution = (ExecutionEntity) executions.get(0);

                String clientId = integrationContext.getClientId();
                String errorClassName = integrationError.getErrorClassName();
                String message =
                    "Received integration error '" +
                    errorClassName +
                    "' with execution id `" +
                    integrationContextEntity.getExecutionId() +
                    ", flow node id `" +
                    clientId +
                    "`. The integration error for the integration context `" +
                    integrationContext.getId() +
                    "` is {}";

                LOGGER.info(message, integrationError);

                if (CloudBpmnError.class.getName().equals(errorClassName)) {
                    if (execution.getActivityId().equals(clientId)) {
                        try {
                            commands.add(new PropagateCloudBpmnErrorCmd(integrationError, execution));
                            commands.add(
                                new AggregateIntegrationErrorReceivedClosingEventCmd(
                                    new AggregateIntegrationErrorReceivedEventCmd(
                                        integrationError,
                                        runtimeBundleProperties,
                                        processEngineEventsAggregator
                                    )
                                )
                            );

                            managementService.executeCommand(CompositeCommand.of(commands.toArray(Command[]::new)));
                            return;
                        } catch (Throwable cause) {
                            LOGGER.error("Error propagating CloudBpmnError: {}", cause.getMessage());
                        }
                    } else {
                        LOGGER.warn(
                            "Could not find matching activityId '{}' for integration error '{}' with executionId '{}'",
                            clientId,
                            integrationError,
                            execution.getId()
                        );
                    }
                }
            } else {
                String message =
                    "No task is in this RB is waiting for integration result with execution id `" +
                    integrationContextEntity.getExecutionId() +
                    ", flow node id `" +
                    integrationContext.getClientId() +
                    "`. The integration result for the integration context `" +
                    integrationContext.getId() +
                    "` will be ignored.";
                LOGGER.warn(message);
            }

            commands.add(
                new AggregateIntegrationErrorReceivedEventCmd(
                    integrationError,
                    runtimeBundleProperties,
                    processEngineEventsAggregator
                )
            );

            managementService.executeCommand(CompositeCommand.of(commands.toArray(Command[]::new)));
        }
    }
}
