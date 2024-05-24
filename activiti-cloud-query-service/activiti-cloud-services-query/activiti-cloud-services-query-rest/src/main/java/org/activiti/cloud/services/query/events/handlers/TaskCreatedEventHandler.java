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
import java.util.Optional;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskCreatedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCreatedEventHandler.class);

    private final EntityManager entityManager;

    private final EntityManagerFinder entityManagerFinder;

    public TaskCreatedEventHandler(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCreatedEvent taskCreatedEvent = CloudTaskCreatedEvent.class.cast(event);
        var taskId = taskCreatedEvent.getEntity().getId();

        Optional
            .ofNullable(entityManager.find(TaskEntity.class, taskId))
            .ifPresentOrElse(
                taskEntity -> LOGGER.warn("Task instance entity already exists for: " + taskId + "!"),
                () -> {
                    TaskEntity queryTaskEntity = new TaskEntity(taskCreatedEvent);

                    if (!queryTaskEntity.isStandalone()) {
                        entityManagerFinder
                            .findProcessInstanceWithTasks(queryTaskEntity.getProcessInstanceId())
                            .ifPresentOrElse(
                                processInstanceEntity -> {
                                    queryTaskEntity.setProcessInstance(processInstanceEntity);
                                    queryTaskEntity.setProcessDefinitionName(
                                        processInstanceEntity.getProcessDefinitionName()
                                    );
                                    queryTaskEntity.setProcessVariables(processInstanceEntity.getVariables());

                                    processInstanceEntity.getTasks().add(queryTaskEntity);
                                },
                                () -> {
                                    throw new QueryException(
                                        "Unable to find task process instance with id: " +
                                        queryTaskEntity.getProcessInstanceId()
                                    );
                                }
                            );
                    }

                    persistIntoDatabase(event, queryTaskEntity);
                }
            );
    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event, TaskEntity queryTaskEntity) {
        try {
            entityManager.persist(queryTaskEntity);
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCreatedEvent[" + event + "]", cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CREATED.name();
    }
}
