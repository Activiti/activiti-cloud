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

package org.activiti.cloud.services.rest.controllers;

import java.util.Date;
import java.util.UUID;

import org.activiti.runtime.api.model.FluentTask;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.impl.FluentTaskImpl;

import static org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED;

public class TaskSamples {

    public static FluentTask buildDefaultAssignedTask() {
        return buildTask("user",
                         ASSIGNED);
    }

    public static FluentTask buildTask(Task.TaskStatus status,
                                       String name,
                                       String assignee) {
        FluentTaskImpl task = buildTask(name,
                                        status);
        task.setAssignee(assignee);
        return task;
    }

    public static FluentTaskImpl buildTask(String name,
                                           Task.TaskStatus status) {
        return buildTask(name,
                         status,
                         UUID.randomUUID().toString(),
                         UUID.randomUUID().toString());
    }

    public static FluentTaskImpl buildStandAloneTask(String name,
                                                     String description) {
        FluentTaskImpl task = buildTask(name,
                                        Task.TaskStatus.CREATED,
                                        null,
                                        null);
        task.setDescription(description);
        return task;
    }

    public static FluentTask buildSubTask(String name,
                                          String description,
                                          String parentTaskId) {
        FluentTaskImpl fluentTask = buildStandAloneTask(name, description);
        fluentTask.setParentTaskId(parentTaskId);
        return fluentTask;
    }

    private static FluentTaskImpl buildTask(String name,
                                            Task.TaskStatus status,
                                            String processInstanceId,
                                            String processDefinitionId) {
        FluentTaskImpl task = new FluentTaskImpl(null,
                                                 null,
                                                 null,
                                                 UUID.randomUUID().toString(),
                                                 name,
                                                 status
        );
        task.setOwner("user");
        task.setDescription("ValidateRequest");
        task.setCreatedDate(new Date());
        task.setDueDate(new Date());
        task.setClaimedDate(new Date());
        task.setPriority(10);
        task.setProcessInstanceId(processInstanceId);
        task.setProcessDefinitionId(processDefinitionId);
        return task;
    }

    public static FluentTaskImpl buildTask(String name,
                                           String description) {
        FluentTaskImpl task = buildTask(name,
                                        Task.TaskStatus.CREATED);
        task.setDescription(description);
        return task;
    }
}
