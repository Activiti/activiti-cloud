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
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.query.model.QTask;
import org.activiti.cloud.services.query.model.Task;
import org.activiti.cloud.services.query.model.Variable;
import org.activiti.cloud.services.query.resources.VariableResource;
import org.activiti.cloud.services.query.rest.assembler.VariableResourceAssembler;
import org.activiti.cloud.services.security.ActivitiForbiddenException;
import org.activiti.cloud.services.security.AuthenticationWrapper;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
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

    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    private AuthenticationWrapper authenticationWrapper;

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
                              SecurityPoliciesApplicationService securityPoliciesApplicationService,
                              AuthenticationWrapper authenticationWrapper,
                              TaskRepository taskRepository,
                              TaskLookupRestrictionService taskLookupRestrictionService) {
        this.variableRepository = variableRepository;
        this.variableResourceAssembler = variableResourceAssembler;
        this.entityFinder = entityFinder;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
        this.authenticationWrapper = authenticationWrapper;
        this.taskRepository = taskRepository;
        this.taskLookupRestrictionService = taskLookupRestrictionService;
    }

    @RequestMapping(value = "/{variableId}", method = RequestMethod.GET)
    public VariableResource findById(@PathVariable long variableId) {

        Variable variable = entityFinder.findById(variableRepository,
                                                  variableId,
                                                  "Unable to find variable for the given id:'" + variableId + "'");

        if (variable.getProcessInstance() != null) {
            ProcessInstance processInstance = variable.getProcessInstance();
            if (!securityPoliciesApplicationService.canRead(processInstance.getProcessDefinitionKey(),
                                                            processInstance.getServiceName())) {
                LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access definition " + processInstance.getProcessDefinitionKey());
                throw new ActivitiForbiddenException("Operation not permitted for " + processInstance.getProcessDefinitionKey());
            }
        }

        if (variable.getTask() != null) {
            Task task = variable.getTask();
            //do restricted query and check if still able to see it
            Iterable<Task> taskIterable = taskRepository.findAll(taskLookupRestrictionService.restrictTaskQuery(QTask.task.id.eq(task.getId())));
            if (!taskIterable.iterator().hasNext()) {
                LOGGER.debug("User " + authenticationWrapper.getAuthenticatedUserId() + " not permitted to access task " + task.getId());
                throw new ActivitiForbiddenException("Operation not permitted for " + task.getId());
            }
        }

        return variableResourceAssembler.toResource(variable);
    }
}
