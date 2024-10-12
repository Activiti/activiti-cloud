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

import jakarta.persistence.EntityManager;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;

public class TaskVariableUpdater {

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public TaskVariableUpdater(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
    }

    public void update(TaskVariableEntity updatedVariableEntity, String notFoundMessage) {
        String taskId = updatedVariableEntity.getTaskId();
        TaskEntity taskEntity = entityManagerFinder
            .findTaskWithVariables(taskId)
            .orElseThrow(() -> new QueryException("Task instance id " + taskId + " not found!"));

        taskEntity
            .getVariable(updatedVariableEntity.getName())
            .ifPresentOrElse(
                variableEntity -> {
                    variableEntity.setLastUpdatedTime(updatedVariableEntity.getLastUpdatedTime());
                    variableEntity.setType(updatedVariableEntity.getType());
                    variableEntity.setValue(updatedVariableEntity.getValue());

                    entityManager.merge(variableEntity);
                },
                () -> {
                    throw new QueryException(notFoundMessage);
                }
            );
    }
}
