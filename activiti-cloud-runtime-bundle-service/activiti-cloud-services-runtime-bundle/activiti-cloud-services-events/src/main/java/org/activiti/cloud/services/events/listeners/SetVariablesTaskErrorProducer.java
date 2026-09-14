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
import org.activiti.cloud.services.events.listeners.ProcessEngineEventsAggregator;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SetVariablesTaskErrorProducer implements ActivitiEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SetVariablesTaskErrorProducer.class);

    private static final String ERROR_MARKER_VARIABLE = "_SET_VARIABLES_TASK_ERROR";
    private static final String ERROR_MESSAGE_VARIABLE = "_SET_VARIABLES_TASK_ERROR_MESSAGE";
    private static final String ERROR_CLASS_VARIABLE = "_SET_VARIABLES_TASK_ERROR_CLASS";

    @Autowired
    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void onEvent(ActivitiEvent event) {
        String executionId = event.getExecutionId();
        if (executionId == null) {
            return;
        }

        try {
            Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();
            if (execution != null) {
                Object errorMarker = runtimeService
                    .getVariables(executionId)
                    .get(ERROR_MARKER_VARIABLE);
                if (errorMarker != null && Boolean.TRUE.equals(errorMarker)) {
                    Object errorMessageObj = runtimeService.getVariables(executionId).get(ERROR_MESSAGE_VARIABLE);
                    Object errorClassNameObj = runtimeService.getVariables(executionId).get(ERROR_CLASS_VARIABLE);

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
                    runtimeService.removeVariable(executionId, ERROR_MARKER_VARIABLE);
                    runtimeService.removeVariable(executionId, ERROR_MESSAGE_VARIABLE);
                    runtimeService.removeVariable(executionId, ERROR_CLASS_VARIABLE);
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
