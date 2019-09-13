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

package org.activiti.cloud.organization.api;

/**
 * Producer of {@link ModelValidationError}
 */
public interface ModelValidationErrorProducer {

    int NAME_MAX_LENGTH = 26;
    String DNS_LABEL_REGEX = "^[a-z]([-a-z0-9]*[a-z0-9])?$";

    String PROJECT_INVALID_EMPTY_NAME = "The project name cannot be empty";
    String PROJECT_INVALID_NAME_LENGTH_MESSAGE =
            "The project name length cannot be greater than " + NAME_MAX_LENGTH;
    String MODEL_INVALID_NAME_LENGTH_MESSAGE =
            "The model name length cannot be greater than " + NAME_MAX_LENGTH;
    String PROJECT_INVALID_NAME_MESSAGE =
            "The project name should follow DNS-1035 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character";
    String MODEL_INVALID_NAME_NULL_MESSAGE = "The model name is required";
    String MODEL_INVALID_NAME_EMPTY_MESSAGE = "The model name cannot be empty";
    String MODEL_INVALID_NAME_MESSAGE =
            "The model name should follow DNS-1035 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character";

    default ModelValidationError createModelValidationError(String problem,
                                                            String description) {
        return createModelValidationError(problem,
                                          description,
                                          null);
    }

    default ModelValidationError createModelValidationError(String problem,
                                                            String description,
                                                            String schema) {
        return createModelValidationError(false,
                                          problem,
                                          description,
                                          schema);
    }

    default ModelValidationError createModelValidationError(boolean warning,
                                                            String problem,
                                                            String description,
                                                            String schema) {
        ModelValidationError modelValidationError = new ModelValidationError();
        modelValidationError.setWarning(warning);
        modelValidationError.setProblem(problem);
        modelValidationError.setDescription(description);
        modelValidationError.setValidatorSetName(schema);
        return modelValidationError;
    }
}
