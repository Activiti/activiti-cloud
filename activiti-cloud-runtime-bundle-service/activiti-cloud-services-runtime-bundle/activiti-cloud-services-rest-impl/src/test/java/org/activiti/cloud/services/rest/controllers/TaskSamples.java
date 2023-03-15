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
package org.activiti.cloud.services.rest.controllers;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;

import java.util.Date;
import java.util.UUID;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;

public class TaskSamples {

    public static Task buildDefaultAssignedTask() {
        return buildTask("user", ASSIGNED);
    }

    public static Task buildTask(Task.TaskStatus status, String name, String assignee) {
        TaskImpl task = buildTask(name, status);
        task.setAssignee(assignee);
        return task;
    }

    public static TaskImpl buildTask(String name, Task.TaskStatus status) {
        return buildTask(name, status, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static TaskImpl buildStandAloneTask(String name, String description) {
        TaskImpl task = buildTask(name, Task.TaskStatus.CREATED, null, null);
        task.setDescription(description);
        return task;
    }

    public static Task buildSubTask(String name, String description, String parentTaskId) {
        TaskImpl fluentTask = buildStandAloneTask(name, description);
        fluentTask.setParentTaskId(parentTaskId);
        return fluentTask;
    }

    private static TaskImpl buildTask(
        String name,
        Task.TaskStatus status,
        String processInstanceId,
        String processDefinitionId
    ) {
        TaskImpl task = new TaskImpl(UUID.randomUUID().toString(), name, status);
        task.setOwner("user");
        task.setDescription("ValidateRequest");
        task.setCreatedDate(new Date());
        task.setDueDate(new Date());
        task.setClaimedDate(new Date());
        task.setPriority(10);
        task.setProcessInstanceId(processInstanceId);
        task.setProcessDefinitionId(processDefinitionId);
        task.setAppVersion("1");
        return task;
    }

    public static TaskImpl buildTask(String name, String description) {
        TaskImpl task = buildTask(name, Task.TaskStatus.CREATED);
        task.setDescription(description);
        return task;
    }
}
