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

import org.activiti.cloud.services.query.model.ProcessVariableEntity;

public class ProcessVariableUpdateEventHandler {

    private final ProcessVariableUpdater variableUpdater;

    public ProcessVariableUpdateEventHandler(ProcessVariableUpdater variableUpdater) {
        this.variableUpdater = variableUpdater;
    }

    public void handle(ProcessVariableEntity updatedVariableEntity) {
        String variableName = updatedVariableEntity.getName();
        String processInstanceId = updatedVariableEntity.getProcessInstanceId();

        variableUpdater.update(
            updatedVariableEntity,
            "Unable to find variable named '" + variableName + "' for process instance '" + processInstanceId + "'"
        );
    }
}
