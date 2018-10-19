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

package org.activiti.cloud.services.organization.rest.api;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.services.organization.rest.config.ApiAlfrescoPageableApi;
import org.activiti.cloud.services.organization.swagger.SwaggerConfiguration.AlfrescoApplicationPage;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.organization.rest.api.ApplicationRestApi.APPLICATIONS;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Controller for {@link Application} resources.
 */
@RestController
@Api(tags = APPLICATIONS, description = "Retrieve and manage application definitions")
@RequestMapping(path = API_VERSION, produces = {HAL_JSON_VALUE, APPLICATION_JSON_VALUE})
public interface ApplicationRestApi {

    String APPLICATIONS = "applications";

    String GET_APPLICATIN_ID_PARAM_DESCR = "The id of the application to retrieve";

    String CREATE_APPLICATION_PARAM_DESCR = "The details of the application to create";

    String UPDATE_APPLICATION_ID_PARAM_DESCR = "The id of the application to update";

    String UPDATE_APPLICATION_PARAM_DESCR = "The new values to update";

    String DELETE_APPLICATION_ID_PARAM_DESCR = "The id of the application to delete";

    String IMPORT_APPLICATION_FILE_PARAM_DESCR = "The file containing the zipped application";

    String EXPORT_APPLICATION_ID_PARAM_DESCR = "The id of the application to export";

    String ATTACHMENT_API_PARAM_DESCR =
            "<b>true</b> value enables a web browser to download the file as an attachment.<br> " +
                    "<b>false</b> means that a web browser may preview the file in a new tab or window, " +
                    "but not download the file.";

    String UPLOAD_FILE_PARAM_NAME = "file";

    String EXPORT_AS_ATTACHMENT_PARAM_NAME = "attachment";

    @ApiOperation(
            tags = APPLICATIONS,
            value = "List application",
            notes = "Get the list of available applications. " +
                    "Minimal information for each application is returned.",
            produces = APPLICATION_JSON_VALUE,
            response = AlfrescoApplicationPage.class)
    @ApiAlfrescoPageableApi
    @GetMapping(path = "/applications")
    PagedResources<Resource<Application>> getApplications(Pageable pageable);

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Create new application")
    @PostMapping(path = "/applications")
    @ResponseStatus(CREATED)
    Resource<Application> createApplication(
            @ApiParam(CREATE_APPLICATION_PARAM_DESCR)
            @RequestBody Application application);

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Get application")
    @GetMapping(path = "/applications/{applicationId}")
    Resource<Application> getApplication(
            @ApiParam(GET_APPLICATIN_ID_PARAM_DESCR)
            @PathVariable String applicationId);

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Update application details")
    @PutMapping(path = "/applications/{applicationId}")
    Resource<Application> updateApplication(
            @ApiParam(UPDATE_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(UPDATE_APPLICATION_PARAM_DESCR)
            @RequestBody Application application);

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Delete application")
    @DeleteMapping(path = "/applications/{applicationId}")
    @ResponseStatus(NO_CONTENT)
    void deleteApplication(
            @ApiParam(DELETE_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId);

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Import an application as zip file",
            notes = "Allows a zip file to be uploaded containing an app definition and any number of included models.")
    @PostMapping(path = "/applications/import", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(CREATED)
    Resource<Application> importApplication(
            @ApiParam(IMPORT_APPLICATION_FILE_PARAM_DESCR)
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file) throws IOException;

    @ApiOperation(
            tags = APPLICATIONS,
            value = "Export an application as zip file",
            notes = "This will synchronously create and download the zip " +
                    "containing the app folder and all related models.<br>" +
                    "Unlike the <b>POST /applications/{applicationId}/export</b> endpoint, " +
                    "this doesn't allow monitoring of the export progress.")
    @GetMapping(path = "/applications/{applicationId}/export")
    void exportApplication(
            HttpServletResponse response,
            @ApiParam(EXPORT_APPLICATION_ID_PARAM_DESCR)
            @PathVariable String applicationId,
            @ApiParam(ATTACHMENT_API_PARAM_DESCR)
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException;
}
