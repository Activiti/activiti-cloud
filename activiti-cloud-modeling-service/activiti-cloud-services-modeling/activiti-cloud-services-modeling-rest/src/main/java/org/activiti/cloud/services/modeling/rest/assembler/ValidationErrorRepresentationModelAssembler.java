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

import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.services.modeling.rest.controller.ModelController;
import org.activiti.cloud.services.modeling.rest.resource.ValidationErrorResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import java.util.ArrayList;

public class ValidationErrorRepresentationModelAssembler extends RepresentationModelAssemblerSupport<ModelValidationError, ValidationErrorResource> {

    public ValidationErrorRepresentationModelAssembler() {
        super(ModelController.class,
              ValidationErrorResource.class);
    }

    @Override
    public ValidationErrorResource toModel(ModelValidationError validationError) {
        return new ValidationErrorResource(validationError,
                                           new ArrayList<>());
    }

    @Override
    public CollectionModel<ValidationErrorResource> toCollectionModel(Iterable<? extends ModelValidationError> entities) {
        return super.toCollectionModel(entities);
    }
}
