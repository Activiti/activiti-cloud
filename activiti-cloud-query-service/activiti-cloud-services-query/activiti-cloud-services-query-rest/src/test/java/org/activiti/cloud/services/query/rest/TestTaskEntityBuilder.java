/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.query.rest;

import java.util.Date;
import java.util.UUID;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TestTaskEntityBuilder {

    public static TaskEntity buildDefaultTask() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(UUID.randomUUID().toString());
        taskEntity.setAssignee("john");
        taskEntity.setName("Review");
        taskEntity.setDescription("Review the report");
        taskEntity.setCreatedDate(new Date());
        taskEntity.setDueDate(new Date());
        taskEntity.setPriority(20);
        taskEntity.setProcessDefinitionId(UUID.randomUUID().toString());
        taskEntity.setProcessInstanceId(UUID.randomUUID().toString());
        taskEntity.setServiceName("My Service");
        taskEntity.setServiceFullName("My Service full name");
        taskEntity.setServiceVersion("1");
        taskEntity.setAppName("My App");
        taskEntity.setAppVersion("2");
        taskEntity.setStatus(Task.TaskStatus.ASSIGNED);
        taskEntity.setLastModified(new Date());
        taskEntity.setClaimedDate(new Date());
        taskEntity.setOwner("peter");
        taskEntity.setFormKey("aFormKey");
        taskEntity.setProcessDefinitionVersion(10);
        taskEntity.setBusinessKey("businessKey");
        taskEntity.setTaskDefinitionKey("taskDefinitionKey");
        return taskEntity;
    }

}
