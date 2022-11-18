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
package org.activiti.cloud.services.modeling.validation;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;

/**
 * Name label validator
 */
public interface NameValidator extends ModelValidationErrorProducer {
    int NAME_MAX_LENGTH = 26;

    String NAME_VALIDATOR = "Name validator";
    String INVALID_REQUIRED_NAME_PROBLEM = "The name is required";
    String INVALID_EMPTY_NAME_PROBLEM = "The name cannot be empty";
    String INVALID_NAME_LENGTH_PROBLEM = "The name length cannot be greater than " + NAME_MAX_LENGTH;

    String INVALID_REQUIRED_NAME_DESCRIPTION = "The %s name is required";
    String INVALID_EMPTY_NAME_DESCRIPTION = "The %s name cannot be empty";
    String INVALID_NAME_LENGTH_DESCRIPTION = "The %s name length cannot be greater than " + NAME_MAX_LENGTH + ": '%s'";

    default Stream<ModelValidationError> validateName(String name, String type) {
        List<ModelValidationError> validationErrors = new ArrayList<>();
        if (name == null) {
            validationErrors.add(
                createModelValidationError(
                    INVALID_REQUIRED_NAME_PROBLEM,
                    format(INVALID_REQUIRED_NAME_DESCRIPTION, type),
                    NAME_VALIDATOR,
                    "field.required"
                )
            );
        } else {
            if (isBlank(name)) {
                validationErrors.add(
                    createModelValidationError(
                        INVALID_EMPTY_NAME_PROBLEM,
                        format(INVALID_EMPTY_NAME_DESCRIPTION, type),
                        NAME_VALIDATOR,
                        "field.empty"
                    )
                );
            }
            if (name.length() > NAME_MAX_LENGTH) {
                validationErrors.add(
                    createModelValidationError(
                        INVALID_NAME_LENGTH_PROBLEM,
                        format(INVALID_NAME_LENGTH_DESCRIPTION, type, name),
                        NAME_VALIDATOR,
                        "length.greater"
                    )
                );
            }
        }

        return validationErrors.stream();
    }
}
