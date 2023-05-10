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
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskAssignedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public TaskAssignedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskAssignedEvent taskAssignedEvent = (CloudTaskAssignedEvent) event;
        Task eventTask = taskAssignedEvent.getEntity();
        Optional<TaskEntity> findResult = Optional.ofNullable(entityManager.find(TaskEntity.class, eventTask.getId()));
        TaskEntity queryTaskEntity = findResult.orElseThrow(() ->
            new QueryException("Unable to find task with id: " + eventTask.getId())
        );
        queryTaskEntity.setAssignee(eventTask.getAssignee());
        queryTaskEntity.setStatus(Task.TaskStatus.ASSIGNED);
        queryTaskEntity.setLastModified(new Date(taskAssignedEvent.getTimestamp()));
        queryTaskEntity.setServiceName(taskAssignedEvent.getServiceName());
        queryTaskEntity.setServiceFullName(taskAssignedEvent.getServiceFullName());
        queryTaskEntity.setServiceVersion(taskAssignedEvent.getServiceVersion());
        queryTaskEntity.setAppName(taskAssignedEvent.getAppName());
        queryTaskEntity.setAppVersion(taskAssignedEvent.getAppVersion());
        queryTaskEntity.setOwner(eventTask.getOwner());
        queryTaskEntity.setClaimedDate(eventTask.getClaimedDate());

        entityManager.persist(queryTaskEntity);
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name();
    }
}
