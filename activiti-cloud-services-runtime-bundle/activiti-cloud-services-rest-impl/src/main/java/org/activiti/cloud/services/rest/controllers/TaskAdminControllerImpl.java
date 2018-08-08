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
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.api.TaskAdminController;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.activiti.runtime.api.TaskAdminRuntime;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.query.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
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
    public PagedResources<TaskResource> getAllTasks(Pageable pageable) {
        Page<Task> tasksPage = taskAdminRuntime.tasks(pageConverter.toAPIPageable(pageable));
        return pagedResourcesAssembler.toResource(pageable,
                                                  pageConverter.toSpringPage(pageable,
                                                                             tasksPage),
                                                  taskResourceAssembler);
    }
}
