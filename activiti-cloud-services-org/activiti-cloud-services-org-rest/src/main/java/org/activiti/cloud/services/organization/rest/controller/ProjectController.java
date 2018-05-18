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

import java.util.stream.Collectors;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.core.model.Group;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.organization.core.repository.ProjectRepository;
import org.activiti.cloud.services.organization.rest.assembler.ProjectResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for {@link Project} resources
 */
@RestController
@RequestMapping(produces = {HAL_JSON_VALUE, APPLICATION_JSON_VALUE})
public class ProjectController {

    private final ProjectRepository projectRepository;

    private final ProjectResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Project> pagedResourcesAssembler;

    private final GroupController groupController;

    @Autowired
    public ProjectController(ProjectRepository projectRepository,
                             ProjectResourceAssembler resourceAssembler,
                             AlfrescoPagedResourcesAssembler<Project> pagedResourcesAssembler,
                             GroupController groupController) {
        this.projectRepository = projectRepository;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.groupController = groupController;
    }

    @RequestMapping(method = GET, path = "/v1/projects")
    public PagedResources<Resource<Project>> getProject(Pageable pageable) {
        return pagedResourcesAssembler.toResource(
                pageable,
                projectRepository.getTopLevelProjects(pageable),
                resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/v1/groups/{groupId}/projects")
    public PagedResources<Resource<Project>> getProjects(@PathVariable String groupId,
                                                         Pageable pageable) {
        Group group = groupController.findGroupById(groupId);
        return pagedResourcesAssembler.toResource(
                pageable,
                projectRepository.getProjects(group,
                                              pageable),
                resourceAssembler);
    }

    @RequestMapping(method = GET, path = "/v1/projects/{projectId}")
    public Resource<Project> getProject(@PathVariable String projectId) {
        return resourceAssembler.toResource(findProjectById(projectId));
    }

    @RequestMapping(method = POST, path = "/v1/groups/{groupId}/projects")
    @ResponseStatus(CREATED)
    public Resource<Project> createProject(@PathVariable String groupId,
                                           @RequestBody Project project) {
        Group group = groupController.findGroupById(groupId);
        return resourceAssembler.toResource(projectRepository.createProject(group,
                                                                            project));
    }

    @RequestMapping(method = POST, path = "/v1/projects")
    @ResponseStatus(CREATED)
    public Resource<Project> createProject(@RequestBody Project project) {
        return resourceAssembler.toResource(projectRepository.createProject(project));
    }

    @RequestMapping(method = PUT, path = "/v1/projects/{projectId}")
    @ResponseStatus(NO_CONTENT)
    public Resource<Project> updateProject(@PathVariable String projectId,
                                           @RequestBody Project project) {
        Project projectToUpdate = findProjectById(projectId);
        return resourceAssembler.toResource(projectRepository.updateProject(projectToUpdate,
                                                                            project));
    }

    @RequestMapping(method = {PUT, PATCH}, path = "/v1/groups/{groupId}/projects", consumes = TEXT_URI_LIST_VALUE)
    @ResponseStatus(NO_CONTENT)
    public void createProjectsReference(@PathVariable String groupId,
                                        @RequestBody Resources<Object> projectsLinks) {
        projectRepository.createProjectsReference(
                groupController.findGroupById(groupId),
                projectsLinks
                        .getLinks()
                        .stream()
                        .map(Link::getHref)
                        .collect(Collectors.toList()));
    }

    @RequestMapping(method = DELETE, path = "/v1/projects/{projectId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteProject(@PathVariable String projectId) {
        projectRepository.deleteProject(findProjectById(projectId));
    }

    public Project findProjectById(String projectId) {
        return projectRepository
                .findProjectById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization project not found: " + projectId));
    }
}
