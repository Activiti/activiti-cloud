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

package org.activiti.cloud.services.modeling.rest.assembler;

import static org.activiti.cloud.modeling.api.ProcessModelType.PROCESS;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.core.error.ModelingException;
import org.activiti.cloud.services.modeling.rest.controller.ModelController;
import org.activiti.cloud.services.modeling.rest.controller.ProjectController;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Assembler for {@link Project} resource
 */
public class ProjectRepresentationModelAssembler implements RepresentationModelAssembler<Project, EntityModel<Project>> {

    @Override
    public EntityModel<Project> toModel(Project project) {
        return new EntityModel<>(
                project,
                linkTo(methodOn(ProjectController.class).getProject(project.getId())).withSelfRel(),
                getExportProjectLink(project.getId()),
                getImportProjectModelLink(project.getId()),
                linkTo(methodOn(ModelController.class).getModels(project.getId(),
                                                                 PROCESS,
                                                                 Pageable.unpaged())).withRel("models"));
    }

    private Link getImportProjectModelLink(String projectId) {
        try {
            return linkTo(methodOn(ModelController.class).importModel(projectId,
                                                                      PROCESS,
                                                                      null)).withRel("import");
        } catch (IOException e) {
            throw new ModelingException(e);
        }
    }

    private Link getExportProjectLink(String projectId) {
        try {
            return linkTo(ProjectController.class,
                          ProjectController.class.getMethod("exportProject",
                                                            HttpServletResponse.class,
                                                            String.class,
                                                            boolean.class),
                          projectId)
                    .withRel("export");
        } catch (NoSuchMethodException e) {
            throw new ModelingException(e);
        }
    }
}
