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

package org.activiti.cloud.qa.service;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;

/**
 * Query Service
 */
public interface QueryService extends BaseService {

    @RequestLine("GET /v1/process-instances/{processInstanceId}")
    @Headers("Content-Type: application/json")
    CloudProcessInstance getProcessInstance(@Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /v1/process-instances")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getAllProcessInstances();

    @RequestLine("GET /admin/v1/process-instances")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getAllProcessInstancesAdmin();

    @RequestLine("GET /v1/tasks?status={status}&id={taskId}")
    PagedResources<CloudTask> queryTasksByIdAnsStatus(@Param("taskId") String taskId,
                                                      @Param("status") Task.TaskStatus taskStatus);

    @RequestLine("GET /v1/tasks?id={taskId}")
    PagedResources<CloudTask> queryTasksById(@Param("taskId") String taskId);

    @RequestLine("GET /v1/tasks")
    @Headers("Content-Type: application/json")
    PagedResources<CloudTask> queryAllTasks();

    @RequestLine("GET /v1/process-instances/{processInstanceId}/variables")
    @Headers("Content-Type: application/json")
    PagedResources<CloudVariableInstance> getProcessInstanceVariables(@Param("processInstanceId") String processInstanceId);

}
