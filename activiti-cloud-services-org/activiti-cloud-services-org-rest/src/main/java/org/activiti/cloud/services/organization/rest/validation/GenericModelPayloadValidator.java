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

package org.activiti.cloud.services.organization.rest.validation;

import org.activiti.cloud.organization.api.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Abstract model payload validator. It contains the basic validation functionality.
 */
public class GenericModelPayloadValidator implements Validator {

    public static final String MODEL_INVALID_NAME_EMPTY_MESSAGE =
            "The model name cannot be empty";

    private final boolean checkRequiredFields;

    public GenericModelPayloadValidator(boolean checkRequiredFields) {
        this.checkRequiredFields = checkRequiredFields;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Model.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target,
                         Errors errors) {
        Model model = (Model) target;
        if (checkRequiredFields || model.getName() != null) {
            validateModelName(model.getName(),
                              errors);
        }
    }

    /**
     * Validate a model name.
     * @param name the model name to validate
     * @param errors the validation errors to update
     */
    public void validateModelName(String name,
                                  Errors errors) {
        if (isEmpty(name)) {
            errors.rejectValue("name",
                               "model.invalid.name.empty",
                               MODEL_INVALID_NAME_EMPTY_MESSAGE);
        }
    }
}
