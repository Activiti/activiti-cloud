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

import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;

public class TaskVariableCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskVariableCreatedEventHandler.class);

    private final EntityManagerFinder entityManagerFinder;
    private final EntityManager entityManager;

    public TaskVariableCreatedEventHandler(EntityManager entityManager,
                                           EntityManagerFinder entityManagerFinder) {
        this.entityManagerFinder = entityManagerFinder;
        this.entityManager = entityManager;
    }

    public void handle(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity = getProcessInstance(variableCreatedEvent);
        String taskId = variableCreatedEvent.getEntity().getTaskId();
        String variableName = variableCreatedEvent.getEntity().getName();

        entityManagerFinder.findTaskWithVariables(taskId)
                           .ifPresentOrElse(taskEntity -> {
                                taskEntity.getVariable(variableName)
                                    .ifPresentOrElse(variableEntity -> {
                                        LOGGER.warn("Variable " + variableName + " already exists in the task " + taskId + "!");
                                    }, () -> {
                                        TaskVariableEntity taskVariableEntity = createTaskVariableEntity(variableCreatedEvent,
                                                                                                         taskEntity,
                                                                                                         processInstanceEntity);
                                        taskEntity.getVariables()
                                                  .add(taskVariableEntity);
                                    });
                           }, () -> {
                                throw new QueryException("Task '" + taskId + "' not found!");
                           });
    }

    private TaskVariableEntity createTaskVariableEntity(CloudVariableCreatedEvent variableCreatedEvent,
                                                        TaskEntity taskEntity,
                                                        ProcessInstanceEntity processInstanceEntity) {
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity(null,
                                                                       variableCreatedEvent.getEntity().getType(),
                                                                       variableCreatedEvent.getEntity().getName(),
                                                                       variableCreatedEvent.getEntity().getProcessInstanceId(),
                                                                       variableCreatedEvent.getServiceName(),
                                                                       variableCreatedEvent.getServiceFullName(),
                                                                       variableCreatedEvent.getServiceVersion(),
                                                                       variableCreatedEvent.getAppName(),
                                                                       variableCreatedEvent.getAppVersion(),
                                                                       variableCreatedEvent.getEntity().getTaskId(),
                                                                       new Date(variableCreatedEvent.getTimestamp()),
                                                                       new Date(variableCreatedEvent.getTimestamp()),
                                                                       null);
        taskVariableEntity.setValue(variableCreatedEvent.getEntity().getValue());
        taskVariableEntity.setProcessInstance(processInstanceEntity);
        taskVariableEntity.setTask(taskEntity);

        entityManager.persist(taskVariableEntity);

        return taskVariableEntity;
    }


    private ProcessInstanceEntity getProcessInstance(CloudVariableCreatedEvent variableCreatedEvent) {
        if (variableCreatedEvent.getEntity().getProcessInstanceId() == null) {
            return null;
        } else {
            return entityManager.getReference(ProcessInstanceEntity.class,
                                              variableCreatedEvent.getEntity().getProcessInstanceId());
        }
    }
}
