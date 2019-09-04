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

import static org.activiti.cloud.organization.validation.ValidationUtil.DNS_LABEL_REGEX;
import static org.activiti.cloud.organization.validation.ValidationUtil.MODEL_INVALID_NAME_LENGTH_MESSAGE;
import static org.activiti.cloud.organization.validation.ValidationUtil.MODEL_INVALID_NAME_MESSAGE;
import static org.activiti.cloud.organization.validation.ValidationUtil.NAME_MAX_LENGTH;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Abstract model payload validator. It contains the basic validation functionality.
 */
public class GenericModelPayloadValidator implements Validator {

    public static final String MODEL_INVALID_NAME_NULL_MESSAGE =
            "The model name is required";

    public static final String MODEL_INVALID_NAME_EMPTY_MESSAGE =
            "The model name cannot be empty";

    private boolean checkRequiredFields;

    public GenericModelPayloadValidator(boolean checkRequiredField) {
        this.checkRequiredFields = checkRequiredField;
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

        if (name == null) {
            errors.rejectValue("name",
                               "field.required",
                               MODEL_INVALID_NAME_NULL_MESSAGE);
        } else {
            if (isEmpty(name)) {
                errors.rejectValue("name",
                                   "field.empty",
                                   MODEL_INVALID_NAME_EMPTY_MESSAGE);
            }
            if (name.length() > NAME_MAX_LENGTH) {
                errors.rejectValue("name",
                                   "length.greater",
                                   MODEL_INVALID_NAME_LENGTH_MESSAGE);
            }
            if (!name.matches(DNS_LABEL_REGEX)) {
                errors.rejectValue("name",
                                   "regex.mismatch",
                                   MODEL_INVALID_NAME_MESSAGE);
            }
        }
    }
}
