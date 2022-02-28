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
package org.activiti.cloud.acc.core.services.query;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;

import java.util.List;

public interface TaskQueryService {

    @RequestLine("GET /v1/tasks?status={status}&id={taskId}")
    PagedModel<CloudTask> queryTasksByIdAnsStatus(
            @Param("taskId") String taskId, @Param("status") Task.TaskStatus taskStatus);

    @RequestLine("GET /v1/tasks?id={taskId}")
    PagedModel<CloudTask> getTask(@Param("taskId") String taskId);

    @RequestLine("GET /v1/tasks?sort=createdDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedModel<CloudTask> getTasks();

    @RequestLine("GET /v1/tasks/{taskId}/variables")
    @Headers("Content-Type: application/json")
    PagedModel<CloudVariableInstance> getTaskVariables(@Param("taskId") String taskId);

    @RequestLine("GET /v1/process-instances/{processInstanceId}/tasks")
    @Headers("Content-Type: application/json")
    PagedModel<CloudTask> getTasksByProcessInstance(
            @Param("processInstanceId") String processInstanceId);

    @RequestLine(
            "GET /v1/tasks?rootTasksOnly=true&processInstanceId={processInstanceId}&sort=createdDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedModel<CloudTask> getRootTasksByProcessInstance(
            @Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /v1/tasks?standalone=true&sort=createdDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getStandaloneTasks();

    @RequestLine("GET /v1/tasks?standalone=false&sort=createdDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getNonStandaloneTasks();

    @RequestLine("GET /v1/tasks?name={taskName}&description={taskDescription}")
    PagedModel<CloudTask> getTasksByNameAndDescription(
            @Param("taskName") String taskName, @Param("taskDescription") String taskDescription);

    @RequestLine("GET /v1/tasks/{id}/variables")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CollectionModel<CloudVariableInstance> getVariables(@Param("id") String id);

    @RequestLine("GET /v1/tasks/{taskId}/candidate-groups")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    public List<String> getTaskCandidateGroups(@Param("taskId") String taskId);

    @RequestLine("GET /v1/tasks/{taskId}/candidate-users")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    public List<String> getTaskCandidateUsers(@Param("taskId") String taskId);
}
