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
package org.activiti.cloud.acc.core.services.query.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

public interface TaskQueryAdminService {
    @RequestLine("GET /admin/v1/tasks/{taskId}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CloudTask getTask(@Param("taskId") String taskId);

    @RequestLine("GET /admin/v1/tasks?sort=createdDate%2Cdesc&sort=id%2Cdesc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getTasks();

    @RequestLine(
        "GET /admin/v1/tasks?rootTasksOnly=true&processInstanceId={processInstanceId}&sort=createdDate%2Cdesc&sort=id%2Cdesc"
    )
    @Headers("Content-Type: application/json")
    PagedModel<CloudTask> getRootTasksByProcessInstance(@Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /admin/v1/tasks?standalone=true&sort=createdDate%2Cdesc&sort=id%2Cdesc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getStandaloneTasks();

    @RequestLine("GET /admin/v1/tasks?standalone=false&sort=createdDate%2Cdesc&sort=id%2Cdesc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getNonStandaloneTasks();

    @RequestLine("DELETE /admin/v1/tasks")
    CollectionModel<EntityModel<CloudTask>> deleteTasks();

    @RequestLine("GET /admin/v1/process-instances/{processInstanceId}/tasks")
    @Headers("Content-Type: application/json")
    PagedModel<CloudTask> getTasksByProcessInstance(@Param("processInstanceId") String processInstanceId);
}
