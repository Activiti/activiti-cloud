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

import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.engine.ActivitiException;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCreatedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskCreatedEventHandler implements QueryEventHandler {

    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    public TaskCreatedEventHandler(TaskRepository taskRepository, EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCreatedEvent taskCreatedEvent = (CloudTaskCreatedEvent) event;
        Task eventEntity = taskCreatedEvent.getEntity();
        org.activiti.cloud.services.query.model.Task queryTask = new org.activiti.cloud.services.query.model.Task(eventEntity.getId(),
                                                                                                             eventEntity.getAssignee(),
                                                                                                             eventEntity.getName(),
                                                                                                             eventEntity.getDescription(),
                                                                                                             eventEntity.getCreatedDate(),
                                                                                                             eventEntity.getDueDate(),
                                                                                                             String.valueOf(eventEntity.getPriority()),
                                                                                                             null,
                                                                                                             eventEntity.getProcessDefinitionId(),
                                                                                                             eventEntity.getProcessInstanceId(),
                                                                                                             event.getServiceName(),
                                                                                                             event.getServiceFullName(),
                                                                                                             event.getServiceVersion(),
                                                                                                             event.getAppName(),
                                                                                                             event.getAppVersion(),
                                                                                                             eventEntity.getStatus().name(),
                                                                                                             eventEntity.getCreatedDate(),
                                                                                                             eventEntity.getClaimedDate(),
                                                                                                             eventEntity.getOwner(),
                                                                                                             eventEntity.getParentTaskId());

        if (!queryTask.isStandAlone()) {
            // Get processInstance reference proxy without database query
            ProcessInstance processInstance = entityManager
                    .getReference(ProcessInstance.class,
                                  queryTask.getProcessInstanceId());

            queryTask.setProcessInstance(processInstance);
        }

        persistIntoDatabase(event,
                            queryTask);
    }

    private void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                     org.activiti.cloud.services.query.model.Task queryTask) {
        try {
            taskRepository.save(queryTask);
        } catch (Exception cause) {
            throw new ActivitiException("Error handling TaskCreatedEvent[" + event + "]",
                                        cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CREATED.name();
    }
}
