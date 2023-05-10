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

import java.util.Date;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskActivatedEvent;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskActivatedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public TaskActivatedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskActivatedEvent taskActivatedEvent = (CloudTaskActivatedEvent) event;
        Task eventTask = taskActivatedEvent.getEntity();
        Optional<TaskEntity> findResult = Optional.ofNullable(entityManager.find(TaskEntity.class, eventTask.getId()));
        TaskEntity taskEntity = findResult.orElseThrow(() ->
            new QueryException("Unable to find taskEntity with id: " + eventTask.getId())
        );
        if (taskEntity.getAssignee() != null && !taskEntity.getAssignee().isEmpty()) {
            taskEntity.setStatus(Task.TaskStatus.ASSIGNED);
        } else {
            taskEntity.setStatus(Task.TaskStatus.CREATED);
        }
        taskEntity.setLastModified(new Date(taskActivatedEvent.getTimestamp()));
        taskEntity.setOwner(taskActivatedEvent.getEntity().getOwner());
        taskEntity.setClaimedDate(taskActivatedEvent.getEntity().getClaimedDate());
        entityManager.persist(taskEntity);
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED.name();
    }
}
