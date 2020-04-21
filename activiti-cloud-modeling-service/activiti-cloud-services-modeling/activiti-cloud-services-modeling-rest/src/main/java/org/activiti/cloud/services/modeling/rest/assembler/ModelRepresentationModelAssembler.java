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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.services.modeling.rest.controller.ModelController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

/**
 * Assembler for {@link Model} resource
 */
public class ModelRepresentationModelAssembler implements RepresentationModelAssembler<Model, EntityModel<Model>> {

    @Override
    public EntityModel<Model> toModel(Model model) {
        return new EntityModel<>(model,
                              linkTo(methodOn(ModelController.class).getModel(model.getId())).withSelfRel());
    }

}
