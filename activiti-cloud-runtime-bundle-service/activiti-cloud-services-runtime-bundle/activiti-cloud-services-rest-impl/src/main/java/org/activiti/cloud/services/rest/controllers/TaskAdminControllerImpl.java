/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskAdminController;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskAdminControllerImpl implements TaskAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final TaskResourceAssembler taskResourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public TaskAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                   TaskResourceAssembler taskResourceAssembler,
                                   AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler,
                                   SpringPageConverter pageConverter) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<Resource<CloudTask>> getTasks(Pageable pageable) {
        Page<Task> tasksPage = taskAdminRuntime.tasks(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             tasksPage),
                                                  taskResourceAssembler);
    }
    
    @Override
    public Resource<CloudTask> getTaskById(@PathVariable String taskId) {
        Task task = taskAdminRuntime.task(taskId);
        return taskResourceAssembler.toResource(task);
    }
    
    @Override
    public Resource<CloudTask> completeTask(@PathVariable String taskId,
                                            @RequestBody(required = false) CompleteTaskPayload completeTaskPayload) {
        if (completeTaskPayload == null) {
            completeTaskPayload = TaskPayloadBuilder
                    .complete()
                    .withTaskId(taskId)
                    .build();
        } else {
            completeTaskPayload.setTaskId(taskId);
        }

        Task task = taskAdminRuntime.complete(completeTaskPayload);
        return taskResourceAssembler.toResource(task);
    }

    @Override
    public Resource<CloudTask> deleteTask(@PathVariable String taskId) {
        Task task = taskAdminRuntime.delete(TaskPayloadBuilder
                                           .delete()
                                           .withTaskId(taskId)
                                           .build());
        return taskResourceAssembler.toResource(task);
    }
    
    @Override
    public Resource<CloudTask> updateTask(@PathVariable String taskId,
                                   @RequestBody UpdateTaskPayload updateTaskPayload) {
        if (updateTaskPayload != null) {
            updateTaskPayload.setTaskId(taskId);
        }
        return taskResourceAssembler.toResource(taskAdminRuntime.update(updateTaskPayload));
    }

    @Override
    public Resource<CloudTask> assign(@PathVariable String taskId,
                               @RequestBody AssignTaskPayload assignTaskPayload) {
        if (assignTaskPayload!=null)
            assignTaskPayload.setTaskId(taskId);
 
        return taskResourceAssembler.toResource(taskAdminRuntime.assign(assignTaskPayload));
    }
}
