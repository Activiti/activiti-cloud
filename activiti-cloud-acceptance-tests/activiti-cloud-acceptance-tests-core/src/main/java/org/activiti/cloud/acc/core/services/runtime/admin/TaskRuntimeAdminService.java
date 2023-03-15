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
package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedModel;

public interface TaskRuntimeAdminService {
    @RequestLine("POST /admin/v1/tasks/{id}/complete")
    @Headers("Content-Type: application/json")
    void completeTask(@Param("id") String id, CompleteTaskPayload createTaskPayload);

    @RequestLine("DELETE /admin/v1/tasks/{id}")
    void deleteTask(@Param("id") String id);

    @RequestLine("PUT /admin/v1/tasks/{taskId}")
    @Headers("Content-Type: application/json")
    CloudTask updateTask(@Param("taskId") String taskId, UpdateTaskPayload updateTaskPayload);

    @RequestLine("POST /admin/v1/{taskId}/assign")
    @Headers("Content-Type: application/json")
    CloudTask assign(@Param("taskId") String taskId, AssignTaskPayload assignTaskPayload);

    @RequestLine("GET /admin/v1/tasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getTasks();
}
