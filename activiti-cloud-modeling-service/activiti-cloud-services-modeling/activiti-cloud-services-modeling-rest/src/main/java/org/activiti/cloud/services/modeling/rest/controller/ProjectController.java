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

package org.activiti.cloud.services.modeling.rest.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import io.swagger.annotations.ApiParam;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.rest.api.ProjectRestApi;
import org.activiti.cloud.services.modeling.rest.assembler.ProjectRepresentationModelAssembler;
import org.activiti.cloud.services.modeling.service.api.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;

/**
 * Controller for {@link Project} resources
 */
@RestController
public class ProjectController implements ProjectRestApi {

    private final ProjectService projectService;

    private final ProjectRepresentationModelAssembler representationModelAssembler;

    private final AlfrescoPagedModelAssembler<Project> pagedCollectionModelAssembler;

    @Autowired
    public ProjectController(ProjectService projectService,
                             ProjectRepresentationModelAssembler representationModelAssembler,
                             AlfrescoPagedModelAssembler<Project> pagedCollectionModelAssembler) {
        this.projectService = projectService;
        this.representationModelAssembler = representationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @Override
    public PagedModel<EntityModel<Project>> getProjects(
            Pageable pageable,
            @RequestParam(
                    name = PROJECT_NAME_PARAM_NAME,
                    required = false) String name) {
        return pagedCollectionModelAssembler.toModel(
                pageable,
                projectService.getProjects(pageable,
                                           name),
                representationModelAssembler);
    }

    @Override
    public EntityModel<Project> getProject(
            @PathVariable String projectId) {
        return representationModelAssembler.toModel(findProjectById(projectId));
    }

    @Override
    public EntityModel<Project> createProject(
            @RequestBody @Valid Project project) {
        return representationModelAssembler.toModel(projectService.createProject(project));
    }

    @Override
    public EntityModel<Project> updateProject(
            @PathVariable String projectId,
            @RequestBody @Valid Project project) {
        Project projectToUpdate = findProjectById(projectId);
        return representationModelAssembler.toModel(projectService.updateProject(projectToUpdate,
                                                                         project));
    }

    @Override
    public void deleteProject(
            @PathVariable String projectId) {
        projectService.deleteProject(findProjectById(projectId));
    }

    @Override
    public EntityModel<Project> importProject(
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file,
            @RequestParam(name = PROJECT_NAME_PARAM_NAME,
                          required = false) String name) throws IOException {
        return representationModelAssembler.toModel(projectService.importProject(file, name));
    }

    @Override
    public void exportProject(
            HttpServletResponse response,
            @PathVariable String projectId,
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Project project = findProjectById(projectId);
        FileContent fileContent = projectService.exportProject(project);
        writeFileToResponse(response,
                            fileContent,
                            attachment);
    }

    @Override
    public void validateProject(
            @ApiParam(VALIDATE_PROJECT_ID_PARAM_DESCR)
            @PathVariable String projectId) throws IOException {
        Project project = findProjectById(projectId);
        projectService.validateProject(project);
    }

    public Project findProjectById(String projectId) {
        return projectService.findProjectById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }
}
