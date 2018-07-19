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

import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudVariableDeleted;
import org.activiti.runtime.api.event.VariableEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VariableDeletedEventHandler implements QueryEventHandler {

    private final ProcessVariableDeletedEventHandler processVariableDeletedHandler;

    private final TaskVariableDeletedEventHandler taskVariableDeletedEventHandler;

    @Autowired
    public VariableDeletedEventHandler(ProcessVariableDeletedEventHandler processVariableDeletedHandler,
                                       TaskVariableDeletedEventHandler taskVariableDeletedEventHandler) {
        this.processVariableDeletedHandler = processVariableDeletedHandler;
        this.taskVariableDeletedEventHandler = taskVariableDeletedEventHandler;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableDeleted variableDeletedEvent = (CloudVariableDeleted) event;
        if (variableDeletedEvent.getEntity().isTaskVariable()) {
            taskVariableDeletedEventHandler.handle(variableDeletedEvent);
        } else {
            processVariableDeletedHandler.handle(variableDeletedEvent);
        }
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_DELETED.name();
    }
}
