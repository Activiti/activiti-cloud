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

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.ApiParam;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.organization.rest.api.ApplicationRestApi;
import org.activiti.cloud.services.organization.rest.assembler.ApplicationResourceAssembler;
import org.activiti.cloud.services.organization.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;

/**
 * Controller for {@link Application} resources
 */
@RestController
public class ApplicationController implements ApplicationRestApi {

    private final ApplicationService applicationService;

    private final ApplicationResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Application> pagedResourcesAssembler;

    @Autowired
    public ApplicationController(ApplicationService applicationService,
                                 ApplicationResourceAssembler resourceAssembler,
                                 AlfrescoPagedResourcesAssembler<Application> pagedResourcesAssembler) {
        this.applicationService = applicationService;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedResources<Resource<Application>> getApplications(Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                applicationService.getApplications(pageable),
                resourceAssembler);
    }

    @Override
    public Resource<Application> getApplication(
            @ApiParam(GET_APPLICATIN_ID_PARAM_DESCR)
            @PathVariable String applicationId) {
        return resourceAssembler.toResource(findApplicationById(applicationId));
    }

    @Override
    public Resource<Application> createApplication(
            @ApiParam(CREATE_APPLICATION_PARAM_DESCR)
            @RequestBody Application application) {
        return resourceAssembler.toResource(applicationService.createApplication(application));
    }

    @Override
    public Resource<Application> updateApplication(
            @ApiParam(UPDATE_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(UPDATE_APPLICATION_PARAM_DESCR)
            @RequestBody Application application) {
        Application applicationToUpdate = findApplicationById(applicationId);
        return resourceAssembler.toResource(applicationService.updateApplication(applicationToUpdate,
                                                                                 application));
    }

    @Override
    public void deleteApplication(
            @ApiParam(DELETE_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId) {
        applicationService.deleteApplication(findApplicationById(applicationId));
    }

    @Override
    public Resource<Application> importApplication(
            @ApiParam(IMPORT_APPLICATION_FILE_PARAM_DESCR)
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException {
        return resourceAssembler.toResource(applicationService.importApplication(file));
    }

    @Override
    public void exportApplication(
            HttpServletResponse response,
            @ApiParam(EXPORT_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(ATTACHMENT_API_PARAM_DESCR)
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Application application = findApplicationById(applicationId);
        FileContent fileContent = applicationService.exportApplication(application);
        writeFileToResponse(response,
                            fileContent,
                            attachment);
    }

    public Application findApplicationById(String applicationId) {
        Optional<Application> optionalApplication = applicationService.findApplicationById(applicationId);
        return optionalApplication
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }
}
