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
package org.activiti.cloud.modeling.api;

import java.util.Objects;

/**
 * Model validation error
 */
public class ModelValidationError {

    private String validatorSetName;
    private String problem;
    private String description;
    private boolean isWarning = false;
    private String errorCode;
    private String referenceId;

    public ModelValidationError() {}

    public ModelValidationError(String problem, String description) {
        this.problem = problem;
        this.description = description;
    }

    public ModelValidationError(String problem, String description, String validatorSetName) {
        this(problem, description);
        this.validatorSetName = validatorSetName;
    }

    public ModelValidationError(String problem, String description, String validatorSetName, boolean isWarning) {
        this(problem, description, validatorSetName);
        this.isWarning = isWarning;
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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModelValidationError)) {
            return false;
        }

        ModelValidationError error = (ModelValidationError) obj;
        return Objects.equals(problem, error.problem) && Objects.equals(description, error.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problem, description);
    }

    @Override
    public String toString() {
        return description;
    }
}
