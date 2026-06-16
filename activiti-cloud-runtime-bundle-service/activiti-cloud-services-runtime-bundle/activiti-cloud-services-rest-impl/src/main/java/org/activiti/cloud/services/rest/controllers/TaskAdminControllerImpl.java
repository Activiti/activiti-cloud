/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import org.activiti.api.task.model.payloads.AssignTasksPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskAdminController;
import org.activiti.cloud.services.rest.assemblers.TaskRepresentationModelAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskAdminControllerImpl implements TaskAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAdminControllerImpl.class);

    private final TaskAdminRuntime taskAdminRuntime;

    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public TaskAdminControllerImpl(
        TaskAdminRuntime taskAdminRuntime,
        TaskRepresentationModelAssembler taskRepresentationModelAssembler,
        AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler,
        SpringPageConverter pageConverter
    ) {
        this.taskAdminRuntime = taskAdminRuntime;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedModel<EntityModel<CloudTask>> getTasks(Pageable pageable) {
        Page<Task> tasksPage = taskAdminRuntime.tasks(pageConverter.toAPIPageable(pageable));
        return pagedCollectionModelAssembler.toModel(
            pageable,
            pageConverter.toSpringPage(pageable, tasksPage),
            taskRepresentationModelAssembler
        );
    }

    @Override
    public EntityModel<CloudTask> getTaskById(@PathVariable String taskId) {
        Task task = taskAdminRuntime.task(taskId);
        return taskRepresentationModelAssembler.toModel(task);
    }

    @Override
    public EntityModel<CloudTask> completeTask(
        @PathVariable String taskId,
        @RequestBody(required = false) CompleteTaskPayload completeTaskPayload
    ) {
        logTaskAttempt("complete", taskId);
        try {
            if (completeTaskPayload == null) {
                completeTaskPayload = TaskPayloadBuilder.complete().withTaskId(taskId).build();
            } else {
                completeTaskPayload.setTaskId(taskId);
            }
            Task task = taskAdminRuntime.complete(completeTaskPayload);
            return taskRepresentationModelAssembler.toModel(task);
        } catch (RuntimeException e) {
            LOGGER.warn("Completing task {} failed", taskId, e);
            throw e;
        }
    }

    @Override
    public EntityModel<CloudTask> deleteTask(@PathVariable String taskId) {
        logTaskAttempt("delete", taskId);
        try {
            Task task = taskAdminRuntime.delete(TaskPayloadBuilder.delete().withTaskId(taskId).build());
            return taskRepresentationModelAssembler.toModel(task);
        } catch (RuntimeException e) {
            LOGGER.warn("Deleting task {} failed", taskId, e);
            throw e;
        }
    }

    @Override
    public EntityModel<CloudTask> updateTask(
        @PathVariable String taskId,
        @RequestBody UpdateTaskPayload updateTaskPayload
    ) {
        logTaskAttempt("update", taskId);
        try {
            if (updateTaskPayload != null) {
                updateTaskPayload.setTaskId(taskId);
            }
            return taskRepresentationModelAssembler.toModel(taskAdminRuntime.update(updateTaskPayload));
        } catch (RuntimeException e) {
            LOGGER.warn("Updating task {} failed", taskId, e);
            throw e;
        }
    }

    @Override
    public EntityModel<CloudTask> assign(
        @PathVariable String taskId,
        @RequestBody AssignTaskPayload assignTaskPayload
    ) {
        logTaskAttempt("assign", taskId);
        try {
            if (assignTaskPayload != null) {
                assignTaskPayload.setTaskId(taskId);
            }
            return taskRepresentationModelAssembler.toModel(taskAdminRuntime.assign(assignTaskPayload));
        } catch (RuntimeException e) {
            LOGGER.warn("Assigning task {} failed", taskId, e);
            throw e;
        }
    }

    @Override
    public PagedModel<EntityModel<CloudTask>> assign(@RequestBody AssignTasksPayload assignTasksPayload) {
        logTaskAttempt("assign multiple tasks");
        try {
            Page<Task> tasks = taskAdminRuntime.assignMultiple(assignTasksPayload);
            Pageable pageable = tasks.getTotalItems() == 0 ? Pageable.unpaged() : Pageable.ofSize(tasks.getTotalItems());
            return pagedCollectionModelAssembler.toModel(
                pageConverter.toSpringPage(pageable, tasks),
                taskRepresentationModelAssembler
            );
        } catch (RuntimeException e) {
            LOGGER.warn("Assigning multiple tasks failed", e);
            throw e;
        }
    }

    private void logTaskAttempt(String action, String taskId) {
        if (LOGGER.isDebugEnabled()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication != null ? authentication.getName() : "unknown";
            LOGGER.debug("User {} wants to {} task {}", userId, action, taskId);
        }
    }

    private void logTaskAttempt(String action) {
        if (LOGGER.isDebugEnabled()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication != null ? authentication.getName() : "unknown";
            LOGGER.debug("User {} wants to {}", userId, action);
        }
    }
}
