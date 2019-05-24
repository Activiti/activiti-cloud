/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.events.handlers;

import java.util.Date;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableUpdatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableUpdatedEventHandler.class);
    
    private ProcessVariableUpdateEventHandler processVariableUpdateEventHandler;

    private TaskVariableUpdatedEventHandler taskVariableUpdatedEventHandler;

    public VariableUpdatedEventHandler(ProcessVariableUpdateEventHandler processVariableUpdateEventHandler,
                                       TaskVariableUpdatedEventHandler taskVariableUpdatedEventHandler) {
        this.processVariableUpdateEventHandler = processVariableUpdateEventHandler;
        this.taskVariableUpdatedEventHandler = taskVariableUpdatedEventHandler;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableUpdatedEvent variableUpdatedEvent = (CloudVariableUpdatedEvent) event;
        
        try {
            if (variableUpdatedEvent.getEntity().isTaskVariable()) {
                TaskVariableEntity variableEntity = new TaskVariableEntity(null,
                                                     variableUpdatedEvent.getEntity().getType(),
                                                     variableUpdatedEvent.getEntity().getName(),
                                                     variableUpdatedEvent.getEntity().getProcessInstanceId(),
                                                     variableUpdatedEvent.getServiceName(),
                                                     variableUpdatedEvent.getServiceFullName(),
                                                     variableUpdatedEvent.getServiceVersion(),
                                                     variableUpdatedEvent.getAppName(),
                                                     variableUpdatedEvent.getAppVersion(),
                                                     variableUpdatedEvent.getEntity().getTaskId(),
                                                     new Date(variableUpdatedEvent.getTimestamp()),
                                                     new Date(variableUpdatedEvent.getTimestamp()),
                                                     null);
                variableEntity.setValue(variableUpdatedEvent.getEntity().getValue());
                taskVariableUpdatedEventHandler.handle(variableEntity);
            }  else {
                ProcessVariableEntity variableEntity = new ProcessVariableEntity(null,
                                                     variableUpdatedEvent.getEntity().getType(),
                                                     variableUpdatedEvent.getEntity().getName(),
                                                     variableUpdatedEvent.getEntity().getProcessInstanceId(),
                                                     variableUpdatedEvent.getServiceName(),
                                                     variableUpdatedEvent.getServiceFullName(),
                                                     variableUpdatedEvent.getServiceVersion(),
                                                     variableUpdatedEvent.getAppName(),
                                                     variableUpdatedEvent.getAppVersion(),
                                                     new Date(variableUpdatedEvent.getTimestamp()),
                                                     new Date(variableUpdatedEvent.getTimestamp()),
                                                     null);
                variableEntity.setValue(variableUpdatedEvent.getEntity().getValue());
                processVariableUpdateEventHandler.handle(variableEntity);
            }
        } catch (Exception cause) {
            LOGGER.debug("Error handling VariableUpdatedEvent[" + event + "]",
                         cause);
        }

    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED.name();
    }
}
