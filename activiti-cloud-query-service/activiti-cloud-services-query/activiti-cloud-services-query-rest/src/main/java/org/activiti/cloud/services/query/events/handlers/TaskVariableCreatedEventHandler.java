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
import org.activiti.cloud.services.query.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;

public class TaskVariableCreatedEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(TaskVariableCreatedEventHandler.class);

    private final EntityManager entityManager;

    public TaskVariableCreatedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void handle(CloudVariableCreatedEvent variableCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity = getProcessInstance(variableCreatedEvent);
        String taskId = variableCreatedEvent.getEntity().getTaskId();
        String variableName = variableCreatedEvent.getEntity().getName();
        TaskEntity taskEntity = Optional.ofNullable(entityManager.find(TaskEntity.class,
                                                                       taskId))
                                        .orElseThrow(() -> new QueryException("Task " + taskId + " not found!"));

        taskEntity.getVariables()
                  .stream()
                  .filter(v -> v.getName().equals(variableName))
                  .findFirst()
                  .ifPresentOrElse(variableEntity -> {
                                       LOGGER.warn("Variable " + variableName + " already exists in the task " + taskId + "!");
                                   },
                                   () -> {
                                       TaskVariableEntity taskVariableEntity = new TaskVariableEntity(null,
                                                                                                      variableCreatedEvent.getEntity().getType(),
                                                                                                      variableName,
                                                                                                      variableCreatedEvent.getEntity().getProcessInstanceId(),
                                                                                                      variableCreatedEvent.getServiceName(),
                                                                                                      variableCreatedEvent.getServiceFullName(),
                                                                                                      variableCreatedEvent.getServiceVersion(),
                                                                                                      variableCreatedEvent.getAppName(),
                                                                                                      variableCreatedEvent.getAppVersion(),
                                                                                                      taskId,
                                                                                                      new Date(variableCreatedEvent.getTimestamp()),
                                                                                                      new Date(variableCreatedEvent.getTimestamp()),
                                                                                                      null);
                                       taskVariableEntity.setValue(variableCreatedEvent.getEntity().getValue());
                                       taskVariableEntity.setProcessInstance(processInstanceEntity);
                                       taskVariableEntity.setTask(taskEntity);
                                       taskEntity.getVariables()
                                                 .add(taskVariableEntity);

                                       entityManager.persist(taskVariableEntity);
                                   });
    }

    private ProcessInstanceEntity getProcessInstance(CloudVariableCreatedEvent variableCreatedEvent) {
        if(variableCreatedEvent.getEntity().getProcessInstanceId() == null){
            return null;
        }else {
            return entityManager.find(ProcessInstanceEntity.class,
                                      variableCreatedEvent.getEntity().getProcessInstanceId());
        }
    }
}
