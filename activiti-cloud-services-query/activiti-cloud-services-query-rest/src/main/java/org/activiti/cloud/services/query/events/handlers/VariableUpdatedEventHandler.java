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

import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableUpdated;
import org.activiti.runtime.api.event.VariableEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VariableUpdatedEventHandler implements QueryEventHandler {

    private ProcessVariableUpdateEventHandler processVariableUpdateEventHandler;

    private TaskVariableUpdatedEventHandler taskVariableUpdatedEventHandler;

    @Autowired
    public VariableUpdatedEventHandler(ProcessVariableUpdateEventHandler processVariableUpdateEventHandler,
                                       TaskVariableUpdatedEventHandler taskVariableUpdatedEventHandler) {
        this.processVariableUpdateEventHandler = processVariableUpdateEventHandler;
        this.taskVariableUpdatedEventHandler = taskVariableUpdatedEventHandler;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableUpdated variableUpdatedEvent = (CloudVariableUpdated) event;
        VariableEntity variableEntity = new VariableEntity(variableUpdatedEvent.getEntity().getType(),
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
        if (variableUpdatedEvent.getEntity().isTaskVariable()) {
            taskVariableUpdatedEventHandler.handle(variableEntity);
        } else {
            processVariableUpdateEventHandler.handle(variableEntity);
        }
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED.name();
    }
}
