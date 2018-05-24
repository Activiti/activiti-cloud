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

package org.activiti.cloud.services.rest.controllers;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceTasksController;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.activiti.runtime.api.model.FluentTask;
import org.activiti.runtime.api.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceTasksControllerImpl implements ProcessInstanceTasksController {

    private final SecurityAwareTaskService securityAwareTaskService;

    private final TaskResourceAssembler taskResourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessInstanceTasksControllerImpl(SecurityAwareTaskService securityAwareTaskService,
                                              TaskResourceAssembler taskResourceAssembler,
                                              AlfrescoPagedResourcesAssembler<Task> pagedResourcesAssembler,
                                              SpringPageConverter pageConverter) {
        this.securityAwareTaskService = securityAwareTaskService;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedResources<TaskResource> getTasks(@PathVariable String processInstanceId,
                                                 Pageable pageable) {
        org.activiti.runtime.api.query.Page<FluentTask> page = securityAwareTaskService.getTasks(processInstanceId,
                                                                                                 pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable, pageConverter.toSpringPage(pageable, page),
                                                  taskResourceAssembler);
    }
}
