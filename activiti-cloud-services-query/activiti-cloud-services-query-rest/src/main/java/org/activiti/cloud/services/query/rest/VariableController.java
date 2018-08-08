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


import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.cloud.services.query.resources.VariableResource;
import org.activiti.cloud.services.query.rest.assembler.VariableResourceAssembler;
import org.activiti.cloud.services.security.ActivitiForbiddenException;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;

import org.activiti.runtime.api.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
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
        value = "/v1/" + VariableRelProvider.COLLECTION_RESOURCE_REL,
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class VariableController {

    private final VariableRepository variableRepository;

    private VariableResourceAssembler variableResourceAssembler;

    private EntityFinder entityFinder;

    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    private SecurityManager securityManager;

    private TaskRepository taskRepository;

    private TaskLookupRestrictionService taskLookupRestrictionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableController.class);

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

    @Autowired
    public VariableController(VariableRepository variableRepository,
                              VariableResourceAssembler variableResourceAssembler,
                              EntityFinder entityFinder,
                              SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService,
                              SecurityManager securityManager,
                              TaskRepository taskRepository,
                              TaskLookupRestrictionService taskLookupRestrictionService) {
        this.variableRepository = variableRepository;
        this.variableResourceAssembler = variableResourceAssembler;
        this.entityFinder = entityFinder;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
        this.securityManager = securityManager;
        this.taskRepository = taskRepository;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
    }

    @RequestMapping(value = "/{variableId}", method = RequestMethod.GET)
    public VariableResource findById(@PathVariable long variableId) {

        VariableEntity variableEntity = entityFinder.findById(variableRepository,
                                                              variableId,
                                                              "Unable to find variableEntity for the given id:'" + variableId + "'");

        if (variableEntity.getProcessInstance() != null) {
            ProcessInstanceEntity processInstanceEntity = variableEntity.getProcessInstance();
            if (!securityPoliciesApplicationService.canRead(processInstanceEntity.getProcessDefinitionKey(),
                                                            processInstanceEntity.getServiceName())) {
                LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access definition " + processInstanceEntity.getProcessDefinitionKey());
                throw new ActivitiForbiddenException("Operation not permitted for " + processInstanceEntity.getProcessDefinitionKey());
            }
        }

        if (variableEntity.getTask() != null) {
            TaskEntity taskEntity = variableEntity.getTask();
            //do restricted query and check if still able to see it
            Iterable<TaskEntity> taskIterable = taskRepository.findAll(taskLookupRestrictionService.restrictTaskQuery(QTaskEntity.taskEntity.id.eq(taskEntity.getId())));
            if (!taskIterable.iterator().hasNext()) {
                LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access taskEntity " + taskEntity.getId());
                throw new ActivitiForbiddenException("Operation not permitted for " + taskEntity.getId());
            }
        }

        return variableResourceAssembler.toResource(variableEntity);
    }
}
