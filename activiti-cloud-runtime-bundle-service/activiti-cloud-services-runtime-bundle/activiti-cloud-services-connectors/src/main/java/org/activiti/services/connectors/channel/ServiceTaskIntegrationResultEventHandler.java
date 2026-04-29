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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.activiti.engine.impl.cmd.TriggerCmd;
import org.activiti.engine.impl.cmd.integration.DeleteIntegrationContextCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

public class ServiceTaskIntegrationResultEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationResultEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final ManagementService managementService;
    private final ProcessEngineEventsAggregator processEngineEventsAggregator;
    private final VariablesPropagator variablesPropagator;
    private final ServiceTaskIntegrationCompletionHandler serviceTaskIntegrationCompletionHandler;

    public ServiceTaskIntegrationResultEventHandler(
        RuntimeService runtimeService,
        IntegrationContextService integrationContextService,
        RuntimeBundleProperties runtimeBundleProperties,
        ManagementService managementService,
        ProcessEngineEventsAggregator processEngineEventsAggregator,
        VariablesPropagator variablesPropagator,
        ServiceTaskIntegrationCompletionHandler serviceTaskIntegrationCompletionHandler
    ) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.managementService = managementService;
        this.processEngineEventsAggregator = processEngineEventsAggregator;
        this.variablesPropagator = variablesPropagator;
        this.serviceTaskIntegrationCompletionHandler = serviceTaskIntegrationCompletionHandler;
    }

    @Retryable(
        value = ActivitiOptimisticLockingException.class,
        maxRetriesString = "${activiti.cloud.integration.result.retry.max-attempts:3}",
        delayString = "${activiti.cloud.integration.result.retry.backoff.delay:0}"
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
                Execution execution = executions.getFirst();

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

            try {
                managementService.executeCommand(CompositeCommand.of(commands.toArray(Command[]::new)));
            } catch (ActivitiOptimisticLockingException e) {
                throw e;
            } catch (Exception triggerException) {
                logDbConcurrencyForensicsIfApplicable(integrationContext, triggerException);
                LOGGER.warn(
                    "Failed to update integration context {}. It might have been already deleted.",
                    integrationContext.getId(),
                    triggerException
                );
                IntegrationRequest fakeRequest = new IntegrationRequestImpl(integrationContext);
                IntegrationErrorImpl integrationError = new IntegrationErrorImpl(fakeRequest, triggerException);
                this.serviceTaskIntegrationCompletionHandler.handlePropagationFailure(
                        integrationError,
                        integrationContextEntity
                    );
            }
        }
    }

    // Diagnostic-only: emits a structured ERROR log for Postgres deadlocks (40P01) and
    // serialization failures (40001) so we can correlate victim/blocker PIDs and the
    // integration context that triggered the conflict. Remove once the deadlock root cause
    // (AAE-42043) is fixed and confirmed in production.
    private static void logDbConcurrencyForensicsIfApplicable(
        IntegrationContext integrationContext,
        Throwable triggerException
    ) {
        SQLException sqlException = findCauseOfType(triggerException, SQLException.class);
        if (sqlException == null) {
            return;
        }
        String sqlState = sqlException.getSQLState();
        if (!isConcurrencyError(sqlState)) {
            return;
        }
        String sqlMessage = sqlException.getMessage();
        LOGGER.error(
            "DB concurrency error in receive(): kind={} sqlState={} " +
            "processInstanceId={} executionId={} clientId={} integrationContextId={} " +
            "processDefinitionId={} victimPgPid={} blockerPgPid={} lockedRelation={} " +
            "mybatisLocation={} pgErrorMessage={}",
            kindOf(sqlState),
            sqlState,
            integrationContext.getProcessInstanceId(),
            integrationContext.getExecutionId(),
            integrationContext.getClientId(),
            integrationContext.getId(),
            integrationContext.getProcessDefinitionId(),
            firstMatch(sqlMessage, PG_VICTIM_PID_PATTERN),
            firstMatch(sqlMessage, PG_BLOCKER_PID_PATTERN),
            firstMatch(sqlMessage, PG_WHERE_PATTERN),
            firstMatchInCauseChain(triggerException, MYBATIS_LOCATION_PATTERN),
            sqlMessage,
            triggerException
        );
    }

    private static <T extends Throwable> T findCauseOfType(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getCause() == current) {
                return null;
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean isConcurrencyError(String sqlState) {
        return DEADLOCK_SQLSTATE.equals(sqlState) || SERIALIZATION_FAILURE_SQLSTATE.equals(sqlState);
    }

    private static String kindOf(String sqlState) {
        if (DEADLOCK_SQLSTATE.equals(sqlState)) {
            return "deadlock";
        }
        if (SERIALIZATION_FAILURE_SQLSTATE.equals(sqlState)) {
            return "serializationFailure";
        }
        return "unknown";
    }

    private static String firstMatch(String text, Pattern pattern) {
        if (text == null) {
            return null;
        }
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String firstMatchInCauseChain(Throwable throwable, Pattern pattern) {
        Throwable current = throwable;
        while (current != null) {
            String match = firstMatch(current.getMessage(), pattern);
            if (match != null) {
                return match;
            }
            if (current.getCause() == current) {
                return null;
            }
            current = current.getCause();
        }
        return null;
    }

    private static final String DEADLOCK_SQLSTATE = "40P01";
    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";

    private static final Pattern PG_VICTIM_PID_PATTERN = Pattern.compile("Process (\\d+) waits for");
    private static final Pattern PG_BLOCKER_PID_PATTERN = Pattern.compile("blocked by process (\\d+)");
    private static final Pattern PG_WHERE_PATTERN = Pattern.compile(
        "while updating tuple \\(\\d+,\\d+\\) in relation \"([^\"]+)\""
    );
    private static final Pattern MYBATIS_LOCATION_PATTERN = Pattern.compile("The error may exist in (\\S+)");
}
