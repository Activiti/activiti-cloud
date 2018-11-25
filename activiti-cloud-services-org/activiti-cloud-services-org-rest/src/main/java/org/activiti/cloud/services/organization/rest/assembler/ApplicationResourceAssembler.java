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

package org.activiti.cloud.services.organization.rest.assembler;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.activiti.cloud.services.organization.rest.controller.ApplicationController;
import org.activiti.cloud.services.organization.rest.controller.ModelController;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Assembler for {@link Application} resource
 */
@Component
public class ApplicationResourceAssembler implements ResourceAssembler<Application, Resource<Application>> {

    @Override
    public Resource<Application> toResource(Application application) {
        return new Resource<>(
                application,
                linkTo(methodOn(ApplicationController.class).getApplication(application.getId())).withSelfRel(),
                getExportApplicationLink(application.getId()),
                getImportApplicationModelLink(application.getId()),
                linkTo(methodOn(ModelController.class).getModels(application.getId(),
                                                                 PROCESS,
                                                                 Pageable.unpaged())).withRel("models"));
    }

    private Link getImportApplicationModelLink(String applicationId) {
        try {
            return linkTo(methodOn(ModelController.class).importModel(applicationId,
                                                                      PROCESS,
                                                                      null)).withRel("import");
        } catch (IOException e) {
            throw new ModelingException(e);
        }
    }

    private Link getExportApplicationLink(String applicationId) {
        try {
            return linkTo(ApplicationController.class,
                          ApplicationController.class.getMethod("exportApplication",
                                                                HttpServletResponse.class,
                                                                String.class,
                                                                boolean.class),
                          applicationId)
                    .withRel("export");
        } catch (NoSuchMethodException e) {
            throw new ModelingException(e);
        }
    }
}
