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
package org.activiti.cloud.services.query.events.handlers;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableCreatedEventHandler.class);

    private final ProcessVariableCreatedEventHandler processVariableCreatedEventHandler;
    private final TaskVariableCreatedEventHandler taskVariableCreatedEventHandler;

    public VariableCreatedEventHandler(
        TaskVariableCreatedEventHandler taskVariableCreatedEventHandler,
        ProcessVariableCreatedEventHandler processVariableCreatedEventHandler
    ) {
        this.taskVariableCreatedEventHandler = taskVariableCreatedEventHandler;
        this.processVariableCreatedEventHandler = processVariableCreatedEventHandler;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudVariableCreatedEvent variableCreatedEvent = (CloudVariableCreatedEvent) event;
        LOGGER.debug("Handling variableEntity created event: " + variableCreatedEvent.getEntity().getName());

        try {
            if (variableCreatedEvent.getEntity().isTaskVariable()) {
                taskVariableCreatedEventHandler.handle(variableCreatedEvent);
            } else {
                processVariableCreatedEventHandler.handle(variableCreatedEvent);
            }
        } catch (Exception cause) {
            LOGGER.error("Error handling VariableCreatedEvent[" + event + "]", cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return VariableEvent.VariableEvents.VARIABLE_CREATED.name();
    }
}
