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

import java.util.Optional;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.QTaskVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskVariableDeletedEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(TaskVariableDeletedEventHandler.class);
    
    private final TaskVariableRepository variableRepository;

    private final EntityFinder entityFinder;
    
    private final TaskRepository taskRepository;

    public TaskVariableDeletedEventHandler(TaskRepository taskRepository,
                                           TaskVariableRepository variableRepository,
                                           EntityFinder entityFinder) {
        this.taskRepository = taskRepository;
        this.variableRepository = variableRepository;
        this.entityFinder = entityFinder;
    }

    public void handle(CloudVariableDeletedEvent event) {
        String variableName = event.getEntity().getName();
        String taskId = event.getEntity().getTaskId();
        Optional<TaskEntity> findResult = taskRepository.findById(taskId);
        
        // if a task was cancelled / completed do not handle this event
        if(findResult.isPresent() && !findResult.get().isInFinalState()) {
            BooleanExpression predicate = QTaskVariableEntity.taskVariableEntity.taskId.eq(taskId)
                    .and(
                            QTaskVariableEntity.taskVariableEntity.name.eq(variableName)
                    );    
            TaskVariableEntity variableEntity = entityFinder.findOne(variableRepository,
                                                                     predicate,
                                                                     "Unable to find variableEntity with name '" + variableName + "' for task '" + taskId + "'");
        
            // Persist into database
            try {
                variableRepository.delete(variableEntity);
            } catch (Exception cause) {
                LOGGER.debug("Error handling TaskVariableDeletedEvent[" + event + "]",
                             cause);
            }                
        }
 
    }
}
