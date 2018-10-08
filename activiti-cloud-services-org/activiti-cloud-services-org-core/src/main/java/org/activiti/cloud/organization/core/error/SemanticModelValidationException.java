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

package org.activiti.cloud.organization.core.error;

import java.util.List;

import org.activiti.cloud.organization.api.ModelValidationError;

/**
 * Exception thrown when semantic errors are found during validating a model content
 */
public class SemanticModelValidationException extends ModelingException {

    private final List<ModelValidationError> validationErrors;

    public SemanticModelValidationException(List<ModelValidationError> validationErrors) {
        super();
        this.validationErrors = validationErrors;
    }

    public SemanticModelValidationException(String message, List<ModelValidationError> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public SemanticModelValidationException(List<ModelValidationError> validationErrors,
                                            Throwable cause) {
        super( cause);
        this.validationErrors = validationErrors;
    }

    public SemanticModelValidationException(String message, List<ModelValidationError> validationErrors,
                                            Throwable cause) {
        super(message, cause);
        this.validationErrors = validationErrors;
    }

    public List<ModelValidationError> getValidationErrors() {
        return validationErrors;
    }
}
