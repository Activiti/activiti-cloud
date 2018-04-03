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

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.events.TaskCancelledEvent;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.engine.ActivitiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TaskCancelledEventHandler
 */
@Component
public class TaskCancelledEventHandler implements QueryEventHandler {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskCancelledEventHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void handle(ProcessEngineEvent event) {
        TaskCancelledEvent taskCancelledEvent = (TaskCancelledEvent) event;
        Task eventTask = taskCancelledEvent.getTask();

        updateTaskStatus(taskRepository
                                 .findById(eventTask.getId())
                                 .orElseThrow(() -> new ActivitiException("Unable to find task with id: " + eventTask.getId())),
                         "CANCELLED",
                         taskCancelledEvent.getTimestamp());
    }

    private void updateTaskStatus(Task task,
                                  String status,
                                  Long eventTimestamp) {
        task.setStatus(status);
        task.setLastModified(new Date(eventTimestamp));
        taskRepository.save(task);
    }

    @Override
    public Class<? extends ProcessEngineEvent> getHandledEventClass() {
        return TaskCancelledEvent.class;
    }
}
