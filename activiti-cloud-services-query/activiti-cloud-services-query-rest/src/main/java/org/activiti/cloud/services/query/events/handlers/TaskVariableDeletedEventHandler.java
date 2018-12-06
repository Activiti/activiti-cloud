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
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskVariableDeletedEventHandler {

    private final TaskVariableRepository variableRepository;

    private final EntityFinder entityFinder;

    @Autowired
    public TaskVariableDeletedEventHandler(TaskVariableRepository variableRepository,
                                           EntityFinder entityFinder) {
        this.variableRepository = variableRepository;
        this.entityFinder = entityFinder;
    }

    public void handle(CloudVariableDeletedEvent event) {
        String variableName = event.getEntity().getName();
        String taskId = event.getEntity().getTaskId();
        BooleanExpression predicate = QTaskVariableEntity.taskVariableEntity.taskId.eq(taskId)
                .and(
                        QTaskVariableEntity.taskVariableEntity.name.eq(variableName)

                ).and(QTaskVariableEntity.taskVariableEntity.markedAsDeleted.eq(Boolean.FALSE));
        TaskVariableEntity variableEntity = entityFinder.findOne(variableRepository,
                                                             predicate,
                                                             "Unable to find variableEntity with name '" + variableName + "' for task '" + taskId + "'");
        variableEntity.setMarkedAsDeleted(true);
        variableRepository.save(variableEntity);
    }
}
