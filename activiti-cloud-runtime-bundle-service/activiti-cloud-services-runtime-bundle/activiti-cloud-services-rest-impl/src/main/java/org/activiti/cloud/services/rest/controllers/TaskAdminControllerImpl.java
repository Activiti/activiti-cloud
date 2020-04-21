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
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskAdminController;
import org.activiti.cloud.services.rest.assemblers.TaskRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskAdminControllerImpl implements TaskAdminController {

    private final TaskAdminRuntime taskAdminRuntime;

    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public TaskAdminControllerImpl(TaskAdminRuntime taskAdminRuntime,
                                   TaskRepresentationModelAssembler taskRepresentationModelAssembler,
                                   AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler,
                                   SpringPageConverter pageConverter) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable) {
        Page<Task> tasksPage = taskAdminRuntime.tasks(pageConverter.toAPIPageable(pageable));
        return pagedCollectionModelAssembler.toModel(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             tasksPage),
                                                  taskRepresentationModelAssembler);
    }

    @Override
    public EntityModel<CloudTask> getTaskById(@PathVariable String taskId) {
        Task task = taskAdminRuntime.task(taskId);
        return taskRepresentationModelAssembler.toModel(task);
    }

    @Override
    public EntityModel<CloudTask> completeTask(@PathVariable String taskId,
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
        return taskRepresentationModelAssembler.toModel(task);
    }

    @Override
    public EntityModel<CloudTask> deleteTask(@PathVariable String taskId) {
        Task task = taskAdminRuntime.delete(TaskPayloadBuilder
                                           .delete()
                                           .withTaskId(taskId)
                                           .build());
        return taskRepresentationModelAssembler.toModel(task);
    }

    @Override
    public EntityModel<CloudTask> updateTask(@PathVariable String taskId,
                                   @RequestBody UpdateTaskPayload updateTaskPayload) {
        if (updateTaskPayload != null) {
            updateTaskPayload.setTaskId(taskId);
        }
        return taskRepresentationModelAssembler.toModel(taskAdminRuntime.update(updateTaskPayload));
    }

    @Override
    public EntityModel<CloudTask> assign(@PathVariable String taskId,
                               @RequestBody AssignTaskPayload assignTaskPayload) {
        if (assignTaskPayload!=null)
            assignTaskPayload.setTaskId(taskId);

        return taskRepresentationModelAssembler.toModel(taskAdminRuntime.assign(assignTaskPayload));
    }
}
