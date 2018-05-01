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

package org.activiti.cloud.services.organization.assemblers;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.organization.core.service.ValidationErrorRepresentation;
import org.activiti.cloud.services.organization.controllers.ValidateModelController;
import org.activiti.cloud.services.organization.resources.ValidationErrorResource;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class ValidationErrorResourceAssembler extends ResourceAssemblerSupport<ValidationErrorRepresentation, ValidationErrorResource> {

    public ValidationErrorResourceAssembler() {
        super(ValidateModelController.class,
              ValidationErrorResource.class);
    }

    @Override
    public ValidationErrorResource toResource(ValidationErrorRepresentation validationError) {
        return new ValidationErrorResource(validationError,
                                           new ArrayList<>());
    }

    @Override
    public List<ValidationErrorResource> toResources(Iterable<? extends ValidationErrorRepresentation> entities) {
        return super.toResources(entities);
    }
}
