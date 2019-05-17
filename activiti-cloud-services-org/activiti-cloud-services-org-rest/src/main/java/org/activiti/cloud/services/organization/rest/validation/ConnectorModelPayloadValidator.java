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

import java.util.Optional;

import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Connector metadata validator
 */
@Component
public class ConnectorModelPayloadValidator implements ModelPayloadValidator {

    public static final String DNS_LABEL_REGEX = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";

    public static final int CONNECTOR_NAME_MAX_LENGTH = 26;

    public static final String CONNECTOR_INVALID_NAME_LENGTH_MESSAGE =
            "The connector name length cannot be greater than " + CONNECTOR_NAME_MAX_LENGTH;

    public static final String CONNECTOR_INVALID_NAME_MESSAGE =
            "The connector name should follow DNS-1123 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character";

    private final ConnectorModelType connectorModelType;

    public ConnectorModelPayloadValidator(ConnectorModelType connectorModelType) {
        this.connectorModelType = connectorModelType;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Model.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target,
                         Errors errors) {
        Model model = (Model) target;

        Optional.ofNullable(model.getName())
                .ifPresent(name -> validateConnectorName(name,
                                                         errors));
    }

    /**
     * Validate a connector name.
     * @param name the connector name to validate
     * @param errors the validation errors to update
     */
    public void validateConnectorName(String name,
                                      Errors errors) {
        if (!isEmpty(name)) {
            if (name.length() > CONNECTOR_NAME_MAX_LENGTH) {
                errors.rejectValue("name",
                                   "connector.invalid.name.length",
                                   CONNECTOR_INVALID_NAME_LENGTH_MESSAGE);
            }

            if (!name.matches(DNS_LABEL_REGEX)) {
                errors.rejectValue("name",
                                   "connector.invalid.name",
                                   CONNECTOR_INVALID_NAME_MESSAGE);
            }
        }
    }

    @Override
    public ModelType getHandledModelType() {
        return connectorModelType;
    }
}
