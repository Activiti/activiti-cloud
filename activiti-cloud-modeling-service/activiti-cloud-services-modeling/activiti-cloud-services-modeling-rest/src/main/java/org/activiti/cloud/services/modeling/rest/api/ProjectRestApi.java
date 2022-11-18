/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.rest.api;

import static org.activiti.cloud.services.modeling.rest.api.ProjectRestApi.PROJECTS;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.activiti.cloud.modeling.api.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for {@link Project} resources.
 */
@RestController
@Tag(name = PROJECTS, description = "Retrieve and manage project definitions")
@RequestMapping(path = "/v1", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
public interface ProjectRestApi {
    String PROJECTS = "projects";

    String GET_PROJECT_ID_PARAM_DESCR = "The id of the project to retrieve";

    String CREATE_PROJECT_PARAM_DESCR = "The details of the project to create";

    String UPDATE_PROJECT_ID_PARAM_DESCR = "The id of the project to update";

    String UPDATE_PROJECT_PARAM_DESCR = "The new values to update";

    String DELETE_PROJECT_ID_PARAM_DESCR = "The id of the project to delete";

    String IMPORT_PROJECT_FILE_PARAM_DESCR = "The file containing the zipped project";

    String EXPORT_PROJECT_ID_PARAM_DESCR = "The id of the project to export";

    String COPY_PROJECT_ID_PARAM_DESCR = "The id of the project to copy";

    String VALIDATE_PROJECT_ID_PARAM_DESCR = "The id of the project to validate";

    String ATTACHMENT_API_PARAM_DESCR =
        "<b>true</b> value enables a web browser to download the file as an attachment.<br> " +
        "<b>false</b> means that a web browser may preview the file in a new tab or window, " +
        "but not download the file.";

    String PROJECT_NAME_PARAM_DESCR = "The name or part of the name to filter projects";

    String PROJECT_NAME_OVERRIDE_DESCR =
        "The name of the project that will override the current name of the project in the zip file";

    String PROJECT_NAME_COPY_DESCR = "The name of the project that will replace the original name of the project";

    String PROJECT_FILTERS_PARAM_DESCR = "The filter name to filter the returned projects";

    String PROJECT_INCLUDE_PARAM_DESCR = "The name of values to include with the returned projects";

    String UPLOAD_FILE_PARAM_NAME = "file";

    String EXPORT_AS_ATTACHMENT_PARAM_NAME = "attachment";

    String PROJECT_NAME_PARAM_NAME = "name";

    String PROJECT_FILTERS_PARAM_NAME = "filters";

    String PROJECT_INCLUDE_PARAM_NAME = "include";

    @Operation(
        tags = PROJECTS,
        summary = "List projects",
        description = "Get the list of available projects. " + "Minimal information for each project is returned."
    )
    @GetMapping(path = "/projects")
    PagedModel<EntityModel<Project>> getProjects(
        Pageable pageable,
        @Parameter(description = PROJECT_NAME_PARAM_DESCR) @RequestParam(
            name = PROJECT_NAME_PARAM_NAME,
            required = false
        ) String name,
        @Parameter(description = PROJECT_FILTERS_PARAM_DESCR) @RequestParam(
            name = PROJECT_FILTERS_PARAM_NAME,
            required = false
        ) List<String> filters,
        @Parameter(description = PROJECT_INCLUDE_PARAM_DESCR) @RequestParam(
            name = PROJECT_INCLUDE_PARAM_NAME,
            required = false
        ) List<String> include
    );

    @Operation(tags = PROJECTS, summary = "Create new project")
    @PostMapping(path = "/projects")
    @ResponseStatus(CREATED)
    EntityModel<Project> createProject(
        @Parameter(description = CREATE_PROJECT_PARAM_DESCR) @RequestBody Project project
    );

    @Operation(tags = PROJECTS, summary = "Get project")
    @GetMapping(path = "/projects/{projectId}")
    EntityModel<Project> getProject(
        @Parameter(description = GET_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = PROJECT_INCLUDE_PARAM_DESCR) @RequestParam(
            name = PROJECT_INCLUDE_PARAM_NAME,
            required = false
        ) List<String> include
    );

    @Operation(tags = PROJECTS, summary = "Update project details")
    @PutMapping(path = "/projects/{projectId}")
    EntityModel<Project> updateProject(
        @Parameter(description = UPDATE_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = UPDATE_PROJECT_PARAM_DESCR) @RequestBody Project project
    );

    @Operation(tags = PROJECTS, summary = "Delete project")
    @DeleteMapping(path = "/projects/{projectId}")
    @ResponseStatus(NO_CONTENT)
    void deleteProject(
        @Parameter(description = DELETE_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId
    );

    @Operation(
        tags = PROJECTS,
        summary = "Import an project as zip file",
        description = "Allows a zip file to be uploaded containing an project definition and any number of included models."
    )
    @PostMapping(path = "/projects/import", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(CREATED)
    EntityModel<Project> importProject(
        @Parameter(description = IMPORT_PROJECT_FILE_PARAM_DESCR) @RequestPart(
            UPLOAD_FILE_PARAM_NAME
        ) MultipartFile file,
        @Parameter(description = PROJECT_NAME_OVERRIDE_DESCR) @RequestParam(
            name = PROJECT_NAME_PARAM_NAME,
            required = false
        ) String name
    ) throws IOException;

    @Operation(
        tags = PROJECTS,
        summary = "Export an project as zip file",
        description = "This will create and download the zip " +
        "containing the project folder and all related models.<br>"
    )
    @GetMapping(path = "/projects/{projectId}/export")
    void exportProject(
        HttpServletResponse response,
        @Parameter(description = EXPORT_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = ATTACHMENT_API_PARAM_DESCR) @RequestParam(
            name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
            required = false,
            defaultValue = "true"
        ) boolean attachment
    ) throws IOException;

    @Operation(
        tags = PROJECTS,
        summary = "Copy an project as a new project with chosen name",
        description = "This will create a new project with chosen name " +
        "containing the project folder and all related models.<br>"
    )
    @PostMapping(path = "/projects/{projectId}/copy")
    EntityModel<Project> copyProject(
        @Parameter(description = COPY_PROJECT_ID_PARAM_DESCR, required = true) @PathVariable String projectId,
        @Parameter(description = PROJECT_NAME_COPY_DESCR) @RequestParam(name = PROJECT_NAME_PARAM_NAME) String name
    );

    @Operation(tags = PROJECTS, summary = "Validate an project by id")
    @GetMapping(path = "/projects/{projectId}/validate")
    void validateProject(@Parameter(description = VALIDATE_PROJECT_ID_PARAM_DESCR) @PathVariable String projectId)
        throws IOException;
}
