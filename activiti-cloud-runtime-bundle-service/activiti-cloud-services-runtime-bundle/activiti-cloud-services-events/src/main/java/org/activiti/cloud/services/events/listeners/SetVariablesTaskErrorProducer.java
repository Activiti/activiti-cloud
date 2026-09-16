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
package org.activiti.cloud.services.events.listeners;

import java.util.ArrayList;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetVariablesTaskErrorProducer implements ActivitiEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SetVariablesTaskErrorProducer.class);

    private static final String ERROR_MARKER_VARIABLE = "_SET_VARIABLES_TASK_ERROR";
    private static final String ERROR_MESSAGE_VARIABLE = "_SET_VARIABLES_TASK_ERROR_MESSAGE";
    private static final String ERROR_CLASS_VARIABLE = "_SET_VARIABLES_TASK_ERROR_CLASS";

    private final ProcessEngineEventsAggregator processEngineEventsAggregator;

    public SetVariablesTaskErrorProducer(ProcessEngineEventsAggregator processEngineEventsAggregator) {
        this.processEngineEventsAggregator = processEngineEventsAggregator;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        String executionId = event.getExecutionId();
        if (executionId == null) {
            return;
        }

        try {
            // Access the execution through the current CommandContext instead of RuntimeService.
            // Going through RuntimeService here would start a nested command execution that
            // reuses (and drains) the agenda of the command currently dispatching this event,
            // causing the in-flight operation to be re-run and this listener to be invoked again
            // recursively, eventually resulting in a StackOverflowError.
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext == null) {
                return;
            }

            ExecutionEntity execution = commandContext.getExecutionEntityManager().findById(executionId);
            if (execution != null) {
                Object errorMarker = execution.getVariable(ERROR_MARKER_VARIABLE);
                if (Boolean.TRUE.equals(errorMarker)) {
                    Object errorMessageObj = execution.getVariable(ERROR_MESSAGE_VARIABLE);
                    Object errorClassNameObj = execution.getVariable(ERROR_CLASS_VARIABLE);

                    String errorMessage = errorMessageObj != null ? errorMessageObj.toString() : null;
                    String errorClassName = errorClassNameObj != null ? errorClassNameObj.toString() : null;

                    publishIntegrationErrorEvent(
                        executionId,
                        event.getProcessInstanceId(),
                        event.getProcessDefinitionId(),
                        errorMessage,
                        errorClassName
                    );

                    // Clean up error variables
                    execution.removeVariable(ERROR_MARKER_VARIABLE);
                    execution.removeVariable(ERROR_MESSAGE_VARIABLE);
                    execution.removeVariable(ERROR_CLASS_VARIABLE);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to process SetVariablesTask error event", e);
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    private void publishIntegrationErrorEvent(
        String executionId,
        String processInstanceId,
        String processDefinitionId,
        String errorMessage,
        String errorClassName
    ) {
        try {
            IntegrationContextImpl integrationContext = new IntegrationContextImpl();
            integrationContext.setId(executionId);
            integrationContext.setExecutionId(executionId);
            integrationContext.setProcessInstanceId(processInstanceId);
            integrationContext.setProcessDefinitionId(processDefinitionId);

            CloudIntegrationErrorReceivedEventImpl event = new CloudIntegrationErrorReceivedEventImpl(
                integrationContext,
                null,
                errorMessage,
                errorClassName,
                new ArrayList<>()
            );

            event.setProcessInstanceId(processInstanceId);
            event.setProcessDefinitionId(processDefinitionId);

            processEngineEventsAggregator.add(event);

            logger.debug("Published integration error event for SetVariablesTask execution {}", executionId);
        } catch (Exception e) {
            logger.warn("Failed to publish integration error event for SetVariablesTask", e);
        }
    }
}
