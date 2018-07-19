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

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.resources.TaskResource;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/v1/process-instances/{processInstanceId}",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceTasksController {

    private TaskResourceAssembler taskResourceAssembler;

    private AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler;

    private final TaskRepository taskRepository;

    @Autowired
    public ProcessInstanceTasksController(TaskRepository taskRepository,
                                          TaskResourceAssembler taskResourceAssembler,
                                          AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler) {
        this.taskRepository = taskRepository;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @RequestMapping(value = "/tasks", method = RequestMethod.GET)
    public PagedResources<TaskResource> getTasks(@PathVariable String processInstanceId,
                                                 Pageable pageable) {
        Page<TaskEntity> page = taskRepository.findAll(QTaskEntity.taskEntity.processInstanceId.eq(processInstanceId),
                                                       pageable);
        return pagedResourcesAssembler.toResource(pageable,
                                                  page,
                                                  taskResourceAssembler);
    }
}
