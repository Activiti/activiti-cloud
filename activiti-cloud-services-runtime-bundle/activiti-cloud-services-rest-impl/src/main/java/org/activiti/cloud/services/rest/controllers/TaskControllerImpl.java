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

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskController;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.runtime.api.NotFoundException;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.builders.TaskPayloadBuilder;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
import org.activiti.runtime.api.model.payloads.CreateTaskPayload;
import org.activiti.runtime.api.model.payloads.UpdateTaskPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskControllerImpl implements TaskController {

    private final TaskResourceAssembler taskResourceAssembler;

    private SpringSecurityAuthenticationWrapper authenticationWrapper;

    private final AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler;

    private final SecurityAwareTaskService securityAwareTaskService;

    private final SpringPageConverter pageConverter;

    @Autowired
    public TaskControllerImpl(TaskResourceAssembler taskResourceAssembler,
                              SpringSecurityAuthenticationWrapper authenticationWrapper,
                              AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler,
                              SecurityAwareTaskService securityAwareTaskService,
                              SpringPageConverter pageConverter) {
        this.authenticationWrapper = authenticationWrapper;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.securityAwareTaskService = securityAwareTaskService;
        this.pageConverter = pageConverter;
    }

    @ExceptionHandler({ActivitiObjectNotFoundException.class, NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(Exception ex) {
        return ex.getMessage();
    }

    @Override
    public PagedResources<TaskResource> getTasks(Pageable pageable) {
        org.activiti.runtime.api.query.Page<Task> taskPage = securityAwareTaskService.getAuthorizedTasks(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             taskPage),
                                                  taskResourceAssembler);
    }

    @Override
    public TaskResource getTaskById(@PathVariable String taskId) {
        Task task = securityAwareTaskService.getTaskById(taskId);
        return taskResourceAssembler.toResource(task);
    }

    @Override
    public TaskResource claimTask(@PathVariable String taskId) {
        String assignee = authenticationWrapper.getAuthenticatedUserId();
        if (assignee == null) {
            throw new IllegalStateException("Assignee must be resolved from the Identity/Security Layer");
        }

        return taskResourceAssembler.toResource(
                securityAwareTaskService.claimTask(
                        TaskPayloadBuilder.claim()
                                .withTaskId(taskId)
                                .withAssignee(assignee)
                                .build()));
    }

    @Override
    public TaskResource releaseTask(@PathVariable String taskId) {

        return taskResourceAssembler.toResource(securityAwareTaskService.releaseTask(TaskPayloadBuilder
                                                                                             .release()
                                                                                             .withTaskId(taskId)
                                                                                             .build()));
    }

    @Override
    public TaskResource completeTask(@PathVariable String taskId,
                                     @RequestBody(required = false) CompleteTaskPayload completeTaskPayload) {
        if (completeTaskPayload != null) {
            completeTaskPayload.setTaskId(taskId);
        }
        Task task = securityAwareTaskService.completeTask(completeTaskPayload);
        return taskResourceAssembler.toResource(task);
    }

    @Override
    public TaskResource deleteTask(@PathVariable String taskId) {
        Task task = securityAwareTaskService.deleteTask(TaskPayloadBuilder
                                                                .delete()
                                                                .withTaskId(taskId)
                                                                .build());
        return taskResourceAssembler.toResource(task);
    }

    @Override
    public TaskResource createNewTask(@RequestBody CreateTaskPayload createTaskPayload) {
        return taskResourceAssembler.toResource(securityAwareTaskService.createNewTask(createTaskPayload));
    }

    @Override
    public TaskResource updateTask(@PathVariable String taskId,
                                   @RequestBody UpdateTaskPayload updateTaskPayload) {
        if (updateTaskPayload != null) {
            updateTaskPayload.setTaskId(taskId);
        }
        return taskResourceAssembler.toResource(securityAwareTaskService.updateTask(updateTaskPayload));
    }

    @Override
    public TaskResource createSubtask(@PathVariable String taskId,
                                      @RequestBody CreateTaskPayload createTaskPayload) {

        return taskResourceAssembler.toResource(securityAwareTaskService.createNewSubtask(taskId,
                                                                                          createTaskPayload));
    }

    @Override
    public PagedResources<TaskResource> getSubtasks(Pageable pageable,
                                                    @PathVariable String taskId) {
        org.activiti.runtime.api.query.Page<Task> taskPage = securityAwareTaskService
                .tasks(pageConverter.toAPIPageable(pageable),
                       TaskPayloadBuilder
                               .tasks()
                               .withParentTaskId(taskId)
                               .build());

        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             taskPage),
                                                  taskResourceAssembler);
    }
}
