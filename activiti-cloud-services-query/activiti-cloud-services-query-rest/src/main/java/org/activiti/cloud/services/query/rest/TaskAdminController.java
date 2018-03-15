/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.services.query.resources.TaskResource;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/admin/v1/tasks",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskAdminController {

    private final TaskRepository taskRepository;

    private TaskResourceAssembler taskResourceAssembler;

    private AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler;

    @Autowired
    public TaskAdminController(TaskRepository taskRepository,
                               TaskResourceAssembler taskResourceAssembler,
                               AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler) {
        this.taskRepository = taskRepository;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(IllegalStateException ex) {
        return ex.getMessage();
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<TaskResource> allTasks(@QuerydslPredicate(root = Task.class) Predicate predicate,
                                                 Pageable pageable) {

        Page<Task> page = taskRepository.findAll(predicate,
                                                 pageable);

        return pagedResourcesAssembler.toResource(pageable,
                                                  page,
                                                  taskResourceAssembler);
    }
}
