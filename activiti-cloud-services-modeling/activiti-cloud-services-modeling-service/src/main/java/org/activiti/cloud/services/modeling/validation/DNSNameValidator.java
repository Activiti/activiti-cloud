/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.modeling.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static java.lang.String.format;

/**
 * DNS label validator
 */
public interface DNSNameValidator extends ModelValidationErrorProducer {

    int NAME_MAX_LENGTH = 26;
    String DNS_LABEL_REGEX = "^[a-z]([-a-z0-9]{0,24}[a-z0-9])?$";

    String DNS_NAME_VALIDATOR = "DNS name validator";
    String INVALID_REQUIRED_NAME_PROBLEM = "The name is required";
    String INVALID_EMPTY_NAME_PROBLEM = "The name cannot be empty";
    String INVALID_NAME_LENGTH_PROBLEM = "The name length cannot be greater than " + NAME_MAX_LENGTH;
    String INVALID_DNS_NAME_PROBLEM = "The name is not a valid DNS name";

    String INVALID_REQUIRED_NAME_DESCRIPTION = "The %s name is required";
    String INVALID_EMPTY_NAME_DESCRIPTION = "The %s name cannot be empty";
    String INVALID_NAME_LENGTH_DESCRIPTION = "The %s name length cannot be greater than " + NAME_MAX_LENGTH + ": '%s'";
    String INVALID_DNS_NAME_DESCRIPTION =
            "The %s name should follow DNS-1035 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character: '%s'";

    default Stream<ModelValidationError> validateDNSName(String name,
                                                         String type) {
        List<ModelValidationError> validationErrors = new ArrayList<>();
        if (name == null) {
            validationErrors.add(createModelValidationError(INVALID_REQUIRED_NAME_PROBLEM,
                                                            format(INVALID_REQUIRED_NAME_DESCRIPTION,
                                                                   type),
                                                            DNS_NAME_VALIDATOR,
                                                            "field.required"));
        } else {
            if (isEmpty(name)) {
                validationErrors.add(createModelValidationError(INVALID_EMPTY_NAME_PROBLEM,
                                                                format(INVALID_EMPTY_NAME_DESCRIPTION,
                                                                       type),
                                                                DNS_NAME_VALIDATOR,
                                                                "field.empty"));
            }
            if (name.length() > NAME_MAX_LENGTH) {
                validationErrors.add(createModelValidationError(INVALID_NAME_LENGTH_PROBLEM,
                                                                format(INVALID_NAME_LENGTH_DESCRIPTION,
                                                                       type,
                                                                       name),
                                                                DNS_NAME_VALIDATOR,
                                                                "length.greater"));
            }
            if (!name.matches(DNS_LABEL_REGEX)) {
                validationErrors.add(createModelValidationError(INVALID_DNS_NAME_PROBLEM,
                                                                format(INVALID_DNS_NAME_DESCRIPTION,
                                                                       type,
                                                                       name),
                                                                DNS_NAME_VALIDATOR,
                                                                "regex.mismatch"));
            }
        }

        return validationErrors.stream();
    }
}
