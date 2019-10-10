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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.services.organization.rest.controller.ModelController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

/**
 * Assembler for {@link Model} resource
 */
public class ModelResourceAssembler implements ResourceAssembler<Model, Resource<Model>> {

    @Override
    public Resource<Model> toResource(Model model) {
        return new Resource<>(model,
                              linkTo(methodOn(ModelController.class).getModel(model.getId())).withSelfRel());
    }
}
