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
import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineEventsAggregator;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
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

    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getExecutionId() == null) {
            return;
        }

        try {
            if (event.getSource() instanceof ExecutionEntity) {
                ExecutionEntity execution = (ExecutionEntity) event.getSource();

                Object errorMarker = execution.getVariable(ERROR_MARKER_VARIABLE);
                if (errorMarker != null && Boolean.TRUE.equals(errorMarker)) {
                    String errorMessage = (String) execution.getVariable(ERROR_MESSAGE_VARIABLE);
                    String errorClassName = (String) execution.getVariable(ERROR_CLASS_VARIABLE);

                    publishIntegrationErrorEvent(execution, errorMessage, errorClassName);

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
        ExecutionEntity execution,
        String errorMessage,
        String errorClassName
    ) {
        try {
            IntegrationContext integrationContext = new IntegrationContextImpl();
            integrationContext.setId(execution.getId());
            integrationContext.setExecutionId(execution.getId());
            integrationContext.setProcessInstanceId(execution.getProcessInstanceId());
            integrationContext.setProcessDefinitionId(execution.getProcessDefinitionId());

            CloudIntegrationErrorReceivedEventImpl event = new CloudIntegrationErrorReceivedEventImpl(
                integrationContext,
                null,
                errorMessage,
                errorClassName,
                new ArrayList<>()
            );

            event.setProcessInstanceId(execution.getProcessInstanceId());
            event.setProcessDefinitionId(execution.getProcessDefinitionId());

            processEngineEventsAggregator.sendCloudEvent(event);

            logger.debug("Published integration error event for SetVariablesTask execution {}", execution.getId());
        } catch (Exception e) {
            logger.warn("Failed to publish integration error event for SetVariablesTask", e);
        }
    }
}
