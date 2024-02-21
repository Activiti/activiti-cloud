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
package org.activiti.cloud.services.rest.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.Parameter;
import org.activiti.api.task.model.payloads.*;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.*;

public interface TaskController {
    @GetMapping("/v1/tasks")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable);

    @GetMapping(value = "/v1/tasks/{taskId}")
    EntityModel<CloudTask> getTaskById(
        @Parameter(description = "Enter the taskId to get task") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(value = "/v1/tasks/{taskId}/claim")
    EntityModel<CloudTask> claimTask(
        @Parameter(description = "Enter the taskId to claim task") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(value = "/v1/tasks/{taskId}/release")
    EntityModel<CloudTask> releaseTask(
        @Parameter(description = "Enter the taskId to release task") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(value = "/v1/tasks/{taskId}/complete", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudTask> completeTask(
        @PathVariable(value = "taskId") String taskId,
        @RequestBody CompleteTaskPayload completeTaskPayload
    );

    @PostMapping(value = "/v1/tasks/{taskId}/save", consumes = APPLICATION_JSON_VALUE)
    void saveTask(
        @Parameter(description = "Enter the taskId to save task") @PathVariable(value = "taskId") String taskId,
        @RequestBody SaveTaskPayload saveTaskPayload
    );

    @DeleteMapping(value = "/v1/tasks/{taskId}")
    EntityModel<CloudTask> deleteTask(
        @Parameter(description = "Enter the taskId to delete task") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(path = "/v1/tasks", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudTask> createNewTask(
        @Parameter(description = "Enter the taskId to create new task") @RequestBody CreateTaskPayload createTaskPayload
    );

    @PutMapping(value = "/v1/tasks/{taskId}", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudTask> updateTask(
        @Parameter(description = "Enter the taskId to update task") @PathVariable(value = "taskId") String taskId,
        @RequestBody UpdateTaskPayload updateTaskPayload
    );

    @GetMapping(value = "/v1/tasks/{taskId}/subtasks")
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudTask>> getSubtasks(
        Pageable pageable,
        @Parameter(description = "Enter the taskId to get sub tasks") @PathVariable(value = "taskId") String taskId
    );

    @PostMapping(value = "/v1/tasks/{taskId}/assign", consumes = APPLICATION_JSON_VALUE)
    EntityModel<CloudTask> assign(
        @Parameter(description = "Enter the taskId to assign task pay load") @PathVariable("taskId") String taskId,
        @RequestBody AssignTaskPayload assignTaskPayload
    );
}
