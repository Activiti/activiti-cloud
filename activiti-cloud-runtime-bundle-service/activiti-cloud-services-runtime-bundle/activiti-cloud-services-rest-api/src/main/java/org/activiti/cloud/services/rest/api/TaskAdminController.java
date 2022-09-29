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
import org.activiti.api.task.model.payloads.AssignTasksPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(value = "/admin/v1/tasks", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface TaskAdminController {

    @RequestMapping( method = RequestMethod.GET)
    PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    EntityModel<CloudTask> getTaskById(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.PUT)
    EntityModel<CloudTask> updateTask(@PathVariable("taskId") String taskId,
                            @RequestBody UpdateTaskPayload updateTaskPayload);

    @RequestMapping(value = "/{taskId}/complete", method = RequestMethod.POST)
    EntityModel<CloudTask> completeTask(@PathVariable String taskId,
                              @RequestBody(required = false) CompleteTaskPayload completeTaskPayload);

    @RequestMapping(value = "/{taskId}", method = RequestMethod.DELETE)
    EntityModel<CloudTask> deleteTask(@PathVariable String taskId);

    @RequestMapping(value = "/{taskId}/assign", method = RequestMethod.POST)
    EntityModel<CloudTask> assign(@PathVariable("taskId") String taskId,
                        @RequestBody AssignTaskPayload assignTaskPayload);

    @PostMapping(value = "/assign", consumes = APPLICATION_JSON_VALUE)
    PagedModel<EntityModel<CloudTask>> assign(@RequestBody AssignTasksPayload assignTasksPayload);

}
