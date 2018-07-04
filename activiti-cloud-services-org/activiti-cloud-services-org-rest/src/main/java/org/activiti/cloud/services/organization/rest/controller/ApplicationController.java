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

package org.activiti.cloud.services.organization.rest.controller;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.core.model.Application;
import org.activiti.cloud.organization.core.repository.ApplicationRepository;
import org.activiti.cloud.services.organization.rest.assembler.ApplicationResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for {@link Application} resources
 */
@RestController
@RequestMapping(produces = {HAL_JSON_VALUE, APPLICATION_JSON_VALUE})
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    private final ApplicationResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Application> pagedResourcesAssembler;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository,
                                 ApplicationResourceAssembler resourceAssembler,
                                 AlfrescoPagedResourcesAssembler<Application> pagedResourcesAssembler) {
        this.applicationRepository = applicationRepository;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @RequestMapping(method = GET, path = "/v1/applications")
    public PagedResources<Resource<Application>> getApplication(Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                applicationRepository.getApplications(pageable),
                resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/v1/applications/{applicationId}")
    public Resource<Application> getApplication(@PathVariable String applicationId) {
        return resourceAssembler.toResource(findApplicationById(applicationId));
    }

    @RequestMapping(method = POST, path = "/v1/applications")
    @ResponseStatus(CREATED)
    public Resource<Application> createApplication(@RequestBody Application application) {
        return resourceAssembler.toResource(applicationRepository.createApplication(application));
    }

    @RequestMapping(method = PUT, path = "/v1/applications/{applicationId}")
    @ResponseStatus(NO_CONTENT)
    public Resource<Application> updateApplication(@PathVariable String applicationId,
                                                   @RequestBody Application application) {
        Application applicationToUpdate = findApplicationById(applicationId);
        return resourceAssembler.toResource(applicationRepository.updateApplication(applicationToUpdate,
                                                                                    application));
    }

    @RequestMapping(method = DELETE, path = "/v1/applications/{applicationId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteApplication(@PathVariable String applicationId) {
        applicationRepository.deleteApplication(findApplicationById(applicationId));
    }

    public Application findApplicationById(String applicationId) {
        return applicationRepository
                .findApplicationById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization application not found: " + applicationId));
    }
}
