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

/**
 * Producer of {@link ModelValidationError}
 */
public interface ModelValidationErrorProducer {
    default ModelValidationError createModelValidationError(
        String problem,
        String description,
        String schema,
        String errorCode
    ) {
        ModelValidationError modelValidationError = new ModelValidationError(problem, description, schema);
        modelValidationError.setErrorCode(errorCode);
        return modelValidationError;
    }

    default ModelValidationError createModelValidationError(
        String problem,
        String description,
        String schema,
        String errorCode,
        String referenceId
    ) {
        ModelValidationError validationError = new ModelValidationError(problem, description, schema);
        validationError.setErrorCode(errorCode);
        validationError.setReferenceId(referenceId);
        return validationError;
    }
}
