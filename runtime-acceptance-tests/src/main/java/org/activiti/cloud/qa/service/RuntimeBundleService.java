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
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.model.commands.CreateTaskCmd;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;

/**
 * Runtime Bundle service
 */
public interface RuntimeBundleService extends BaseService {

    String PROCESS_INSTANCES_PATH = "/v1/process-instances/";

    String TASKS_PATH = "/v1/tasks/";

    @RequestLine("POST /v1/process-instances")
    @Headers("Content-Type: application/json")
    ProcessInstance startProcess(ProcessInstance processInstance);

    @RequestLine("POST /v1/process-instances/{id}/suspend")
    @Headers("Content-Type: application/json")
    void suspendProcess(@Param("id") String id);

    @RequestLine("POST /v1/process-instances/{id}/activate")
    @Headers("Content-Type: application/json")
    void activateProcess(@Param("id") String id);

    @RequestLine("GET /v1/process-instances/{id}/tasks")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/hal+json;charset=UTF-8"
    })
    PagedResources<Task> getProcessInstanceTasks(@Param("id") String id);

    @RequestLine("POST /v1/tasks/{id}/claim")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    void assignTaskToUser(@Param("id") String id,
                          @Param("assignee") String user);

    @RequestLine("POST /v1/tasks/{id}/complete")
    @Headers("Content-Type: application/json")
    void completeTask(@Param("id") String id);

    @RequestLine("DELETE /v1/process-instances/{id}")
    @Headers("Content-Type: application/json")
    void deleteProcess(@Param("id") String id);

    @RequestLine("GET /v1/process-instances/{id}")
    @Headers("Content-Type: application/json")
    ProcessInstance getProcessInstance(@Param("id") String id);

    @RequestLine("POST /v1/tasks/")
    @Headers("Content-Type: application/json")
    Task createNewTask(CreateTaskCmd task);

    @RequestLine("GET /v1/tasks/{id}")
    Task getTaskById(@Param("id") String id);

    @RequestLine("DELETE /v1/tasks/{id}")
    void deleteTask(@Param("id") String id);

    @RequestLine("POST /v1/tasks/{parentTaskId}/subtask")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/hal+json;charset=UTF-8"
    })
    Task createSubtask(@Param("parentTaskId") String parentTaskId,
                       CreateTaskCmd createTaskCmd);

    @RequestLine("GET /v1/tasks/{parentTaskId}/subtasks")
    Resources getSubtasks(@Param("parentTaskId") String parentTaskId);
}
