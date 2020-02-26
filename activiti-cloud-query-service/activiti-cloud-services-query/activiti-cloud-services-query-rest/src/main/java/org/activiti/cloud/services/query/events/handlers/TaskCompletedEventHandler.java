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

import java.util.Date;
import java.util.Optional;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCompletedEvent;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskCompletedEventHandler implements QueryEventHandler {

    private final TaskRepository taskRepository;

    public TaskCompletedEventHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudTaskCompletedEvent taskCompletedEvent = (CloudTaskCompletedEvent) event;
        Task eventTask = taskCompletedEvent.getEntity();
        Optional<TaskEntity> findResult = taskRepository.findById(eventTask.getId());
        TaskEntity queryTaskEntity = findResult.orElseThrow(
                () -> new QueryException("Unable to find task with id: " + eventTask.getId())
        );

        queryTaskEntity.setStatus(eventTask.getStatus());
        queryTaskEntity.setLastModified(new Date(taskCompletedEvent.getTimestamp()));
        queryTaskEntity.setCompletedDate(new Date (taskCompletedEvent.getTimestamp()));
        
        if (queryTaskEntity.getCompletedDate() != null && queryTaskEntity.getCreatedDate() != null) {
            queryTaskEntity.setDuration(queryTaskEntity.getCompletedDate().getTime() - queryTaskEntity.getCreatedDate().getTime());        
        }

        taskRepository.save(queryTaskEntity);
    }

    @Override
    public String getHandledEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name();
    }
}
