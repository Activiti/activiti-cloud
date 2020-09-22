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

import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/v1/tasks", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskController {

    @GetMapping()
    PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable);

    @GetMapping(value = "/{taskId}")
    EntityModel<CloudTask> getTaskById(@PathVariable(value = "taskId") String taskId);

    @PostMapping(value = "/{taskId}/claim")
    EntityModel<CloudTask> claimTask(@PathVariable(value = "taskId") String taskId);

    @PostMapping(value = "/{taskId}/release")
    EntityModel<CloudTask> releaseTask(@PathVariable(value = "taskId") String taskId);

    @PostMapping(value = "/{taskId}/complete",
        headers = "Content-type=application/json")
    EntityModel<CloudTask> completeTask(@PathVariable(value = "taskId") String taskId,
        @RequestBody CompleteTaskPayload completeTaskPayload);

    @PostMapping(value = "/{taskId}/save",
        headers = "Content-type=application/json")
    void saveTask(@PathVariable(value = "taskId") String taskId,
        @RequestBody SaveTaskPayload saveTaskPayload);

    @DeleteMapping(value = "/{taskId}")
    EntityModel<CloudTask> deleteTask(@PathVariable(value = "taskId") String taskId);

    @PostMapping(headers = "Content-type=application/json")
    EntityModel<CloudTask> createNewTask(@RequestBody CreateTaskPayload createTaskPayload);

    @PutMapping(value = "/{taskId}",
        headers = "Content-type=application/json")
    EntityModel<CloudTask> updateTask(@PathVariable(value = "taskId") String taskId,
        @RequestBody UpdateTaskPayload updateTaskPayload);

    @GetMapping(value = "/{taskId}/subtasks")
    PagedModel<EntityModel<CloudTask>> getSubtasks(Pageable pageable, @PathVariable(value = "taskId") String taskId);

    @PostMapping(value = "/{taskId}/assign")
    EntityModel<CloudTask> assign(@PathVariable("taskId") String taskId,
                                  @RequestBody AssignTaskPayload assignTaskPayload);
}
