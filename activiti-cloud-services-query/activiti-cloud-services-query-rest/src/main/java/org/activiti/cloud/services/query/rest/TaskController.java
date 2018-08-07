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
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.resources.TaskResource;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.activiti.cloud.services.security.ActivitiForbiddenException;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.runtime.api.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/v1/" + TaskRelProvider.COLLECTION_RESOURCE_REL,
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskController {

    private final TaskRepository taskRepository;

    private TaskResourceAssembler taskResourceAssembler;

    private AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler;

    private EntityFinder entityFinder;

    private TaskLookupRestrictionService taskLookupRestrictionService;

    private SecurityManager securityManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    public TaskController(TaskRepository taskRepository,
                          TaskResourceAssembler taskResourceAssembler,
                          AlfrescoPagedResourcesAssembler<TaskEntity> pagedResourcesAssembler,
                          EntityFinder entityFinder,
                          TaskLookupRestrictionService taskLookupRestrictionService,
                          SecurityManager securityManager) {
        this.taskRepository = taskRepository;
        this.taskResourceAssembler = taskResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.entityFinder = entityFinder;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
        this.securityManager = securityManager;
    }

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(IllegalStateException ex) {
        return ex.getMessage();
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<TaskResource> findAll(@QuerydslPredicate(root = TaskEntity.class) Predicate predicate,
                                                Pageable pageable) {

        Predicate extendedPredicate = taskLookupRestrictionService.restrictTaskQuery(predicate);
        Page<TaskEntity> page = taskRepository.findAll(extendedPredicate,
                                                       pageable);

        return pagedResourcesAssembler.toResource(pageable,
                                                  page,
                                                  taskResourceAssembler);
    }

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET)
    public TaskResource findById(@PathVariable String taskId) {

        TaskEntity taskEntity = entityFinder.findById(taskRepository,
                                                      taskId,
                                                      "Unable to find taskEntity for the given id:'" + taskId + "'");

        //do restricted query and check if still able to see it
        Iterable<TaskEntity> taskIterable = taskRepository.findAll(taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq(taskId)));
        if (!taskIterable.iterator().hasNext()) {
            LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access taskEntity " + taskId);
            throw new ActivitiForbiddenException("Operation not permitted for " + taskId);
        }

        return taskResourceAssembler.toResource(taskEntity);
    }
}
