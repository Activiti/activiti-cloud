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

import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskVariableUpdatedEventHandler {

    private final TaskVariableUpdater variableUpdater;

    @Autowired
    public TaskVariableUpdatedEventHandler(TaskVariableUpdater variableUpdater) {
        this.variableUpdater = variableUpdater;
    }

    public void handle(TaskVariableEntity updatedVariableEntity) {
        String variableName = updatedVariableEntity.getName();
        String taskId = updatedVariableEntity.getTaskId();
        BooleanExpression predicate = QTaskVariableEntity.taskVariableEntity.name.eq(variableName)
                .and(
                        QTaskVariableEntity.taskVariableEntity.taskId.eq(String.valueOf(taskId))
                );
        variableUpdater.update(updatedVariableEntity,
                               predicate,
                               "Unable to find variable named '" + variableName + "' for task '" + taskId + "'");
    }
}
