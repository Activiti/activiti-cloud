/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.rest.validation;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.services.modeling.validation.NameValidator;
import org.activiti.cloud.services.modeling.validation.model.ModelNameValidator;
import org.springframework.validation.Errors;

/**
 * Abstract model payload validator. It contains the basic validation functionality.
 */
public class ModelPayloadValidator extends GenericPayloadValidator<Model> implements NameValidator {

    private ModelNameValidator modelNameValidator;

    public ModelPayloadValidator(boolean validateRequiredFields, ModelNameValidator modelNameValidator) {
        super(Model.class, validateRequiredFields);
        this.modelNameValidator = modelNameValidator;
    }

    @Override
    public void validatePayload(Model model, Errors errors) {
        if (validateRequiredFields || model.getDisplayName() != null) {
            modelNameValidator
                .validateName(model)
                .forEach(error -> errors.rejectValue("name", error.getErrorCode(), error.getDescription()));
        }
    }
}
