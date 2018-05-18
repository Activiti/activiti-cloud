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

package org.activiti.cloud.organization.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.activiti.validation.ValidationError;

/**
 * Representation for {@link ValidationError} with relevant fields for clients
 */
public class ValidationErrorRepresentation {

    private String validatorSetName;
    private String problem;
    private String description;
    private boolean isWarning;

    @JsonCreator
    public ValidationErrorRepresentation(ValidationError validationError) {
        this.validatorSetName = validationError.getValidatorSetName();
        this.problem = validationError.getProblem();
        this.description = validationError.getDefaultDescription();
        this.isWarning = validationError.isWarning();
    }

    public String getValidatorSetName() {
        return validatorSetName;
    }

    public void setValidatorSetName(String validatorSetName) {
        this.validatorSetName = validatorSetName;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setWarning(boolean warning) {
        isWarning = warning;
    }
}
