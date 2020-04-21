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

import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.ProcessInstanceTasksController;
import org.activiti.cloud.services.rest.assemblers.TaskRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstanceTasksControllerImpl implements ProcessInstanceTasksController {

    private final TaskRuntime taskRuntime;

    private final TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    private final AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler;

    private final SpringPageConverter pageConverter;

    @Autowired
    public ProcessInstanceTasksControllerImpl(TaskRuntime taskRuntime,
                                              TaskRepresentationModelAssembler taskRepresentationModelAssembler,
                                              AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler,
                                              SpringPageConverter pageConverter) {
        this.taskRuntime = taskRuntime;
        this.taskRepresentationModelAssembler = taskRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.pageConverter = pageConverter;
    }

    @Override
    public PagedModel<EntityModel<CloudTask>> getTasks(@PathVariable String processInstanceId,
                                                        Pageable pageable) {
        Page<Task> page = taskRuntime.tasks(pageConverter.toAPIPageable(pageable),
                                            TaskPayloadBuilder.tasks()
                                                                                                .withProcessInstanceId(processInstanceId)
                                                                                                .build());
        return pagedCollectionModelAssembler.toModel(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             page),
                                                  taskRepresentationModelAssembler);
    }
}
