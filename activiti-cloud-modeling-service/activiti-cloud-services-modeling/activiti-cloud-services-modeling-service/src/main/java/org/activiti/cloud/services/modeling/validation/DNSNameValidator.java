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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.activiti.cloud.modeling.api.ModelValidationError;

/**
 * DNS label validator
 */
public interface DNSNameValidator extends NameValidator {
    String DNS_LABEL_REGEX = "^[a-z]([-a-z0-9]{0,24}[a-z0-9])?$";
    String DNS_LABEL_REGEX_WITHOUT_LENGTH = "^[a-z]([-a-z0-9]*[a-z0-9])?$";
    String DNS_NAME_VALIDATOR = "DNS name validator";
    String INVALID_DNS_NAME_PROBLEM = "The name is not a valid DNS name";

    String INVALID_DNS_NAME_DESCRIPTION =
        "The %s name should follow DNS-1035 conventions: " +
        "it must consist of lower case alphanumeric characters or '-', " +
        "and must start and end with an alphanumeric character: '%s'";

    default Stream<ModelValidationError> validateDNSName(String name, String type) {
        Stream<ModelValidationError> validationErrors = validateName(name, type);
        if (name != null && !name.matches(DNS_LABEL_REGEX)) {
            ModelValidationError dnsNameValidatorError = createModelValidationError(
                INVALID_DNS_NAME_PROBLEM,
                format(INVALID_DNS_NAME_DESCRIPTION, type, name),
                DNS_NAME_VALIDATOR,
                "regex.mismatch"
            );
            validationErrors = Stream.concat(validationErrors, Stream.of(dnsNameValidatorError));
        }
        return validationErrors;
    }

    default Stream<ModelValidationError> validateDNSNameCharacters(String name, String type) {
        List<ModelValidationError> validationErrors = new ArrayList<>();
        if (name != null && !name.matches(DNS_LABEL_REGEX_WITHOUT_LENGTH)) {
            ModelValidationError dnsNameValidatorError = createModelValidationError(
                INVALID_DNS_NAME_PROBLEM,
                format(INVALID_DNS_NAME_DESCRIPTION, type, name),
                DNS_NAME_VALIDATOR,
                "regex.mismatch"
            );
            validationErrors.add(dnsNameValidatorError);
        }
        return validationErrors.stream();
    }
}
