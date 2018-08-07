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
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.resources.ProcessInstanceResource;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceResourceAssembler;
import org.activiti.cloud.services.security.ActivitiForbiddenException;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.runtime.api.security.SecurityManager;
import org.activiti.spring.security.policies.SecurityPolicyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        value = "/v1/" + ProcessInstanceRelProvider.COLLECTION_RESOURCE_REL,
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceResourceAssembler processInstanceResourceAssembler;

    private AlfrescoPagedResourcesAssembler<ProcessInstanceEntity> pagedResourcesAssembler;

    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    private SecurityManager securityManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceController.class);

    private EntityFinder entityFinder;

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
    public ProcessInstanceController(ProcessInstanceRepository processInstanceRepository,
                                     ProcessInstanceResourceAssembler processInstanceResourceAssembler,
                                     AlfrescoPagedResourcesAssembler<ProcessInstanceEntity> pagedResourcesAssembler,
                                     EntityFinder entityFinder,
                                     SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService,
                                     SecurityManager securityManager) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceResourceAssembler = processInstanceResourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.entityFinder = entityFinder;
        this.securityPoliciesApplicationService = securityPoliciesApplicationService;
        this.securityManager = securityManager;
    }

    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<ProcessInstanceResource> findAll(@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
                                                           Pageable pageable) {

        predicate = securityPoliciesApplicationService.restrictProcessInstanceQuery(predicate,
                SecurityPolicyAccess.READ);

        return pagedResourcesAssembler.toResource(pageable,
                                                  processInstanceRepository.findAll(predicate,
                                                                                    pageable),
                                                  processInstanceResourceAssembler);
    }

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    public ProcessInstanceResource findById(@PathVariable String processInstanceId) {

        ProcessInstanceEntity processInstanceEntity = entityFinder.findById(processInstanceRepository,
                                                                            processInstanceId,
                                                                            "Unable to find task for the given id:'" + processInstanceId + "'");

        if (!securityPoliciesApplicationService.canRead(processInstanceEntity.getProcessDefinitionKey(),
                                                        processInstanceEntity.getServiceName())) {
            LOGGER.debug("User " + securityManager.getAuthenticatedUserId() + " not permitted to access definition " + processInstanceEntity.getProcessDefinitionKey());
            throw new ActivitiForbiddenException("Operation not permitted for " + processInstanceEntity.getProcessDefinitionKey());
        }

        return processInstanceResourceAssembler.toResource(processInstanceEntity);
    }
}
