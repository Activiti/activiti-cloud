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

package org.activiti.cloud.services.organization.validation;

import java.util.List;

import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.rest.client.service.ModelReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@link ModelValidator} implementation for {@link ProcessModelType}
 */
@Component
public class ProcessModelValidator implements ModelValidator {

    private final ProcessModelType processModelType;

    private final ModelReferenceService modelReferenceService;

    @Autowired
    public ProcessModelValidator(ProcessModelType processModelType,
                                 ModelReferenceService modelReferenceService) {
        this.processModelType = processModelType;
        this.modelReferenceService = modelReferenceService;
    }

    @Override
    public void validateModelContent(byte[] modelContent) {
        List<ModelValidationError> validationErrors = modelReferenceService
                .validateResourceContent(processModelType.getName(),
                                         modelContent);
        if (!validationErrors.isEmpty()) {
            throw new SemanticModelValidationException("Semantic process model validation errors encountered",
                                                       validationErrors);
        }
    }

    @Override
    public ModelType getHandledModelType() {
        return processModelType;
    }
}
