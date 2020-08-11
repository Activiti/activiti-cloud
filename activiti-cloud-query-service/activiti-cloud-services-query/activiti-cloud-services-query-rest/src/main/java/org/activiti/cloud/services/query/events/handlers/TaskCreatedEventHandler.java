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

import javax.persistence.EntityManager;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;
public class TaskCreatedEventHandler implements QueryEventHandler {

    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    public TaskCreatedEventHandler(TaskRepository taskRepository,
                                   EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCreatedEvent taskCreatedEvent = (CloudTaskCreatedEvent) event;
        TaskEntity queryTaskEntity = new TaskEntity(taskCreatedEvent);

        if (!queryTaskEntity.isStandalone()) {
            // Get processInstanceEntity reference proxy without database query
            ProcessInstanceEntity processInstanceEntity = entityManager
                    .getReference(ProcessInstanceEntity.class,
                                  queryTaskEntity.getProcessInstanceId());

            
            queryTaskEntity.setProcessInstance(processInstanceEntity);
            queryTaskEntity.setProcessDefinitionName(processInstanceEntity.getProcessDefinitionName());
        }

        persistIntoDatabase(event,
                            queryTaskEntity);
    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                     TaskEntity queryTaskEntity) {
        try {
            taskRepository.save(queryTaskEntity);
        } catch (Exception cause) {
            throw new QueryException("Error handling TaskCreatedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CREATED.name();
    }
}
