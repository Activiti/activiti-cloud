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

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.services.organization.rest.controller.ModelController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Assembler for {@link Model} resource
 */
@Component
public class ModelResourceAssembler implements ResourceAssembler<Model, Resource<Model>> {

    private final ResourceProcessor<Resource<?>> resourceProcessor;

    public ModelResourceAssembler(ResourceProcessor<Resource<?>> resourceProcessors) {
        this.resourceProcessor = resourceProcessors;
    }

    @Override
    public Resource<Model> toResource(Model model) {
        Resource<Model> modelResource = new Resource<>(
                model,
                linkTo(methodOn(ModelController.class).getModel(model.getId())).withSelfRel());
        return (Resource<Model>) resourceProcessor.process(modelResource);
    }
}
