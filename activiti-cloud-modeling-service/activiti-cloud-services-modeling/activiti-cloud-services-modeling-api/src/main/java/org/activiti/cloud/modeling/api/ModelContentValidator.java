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

import java.util.Collection;

/**
 * Business logic related with validation of a model content
 */
public interface ModelContentValidator extends ModelValidator {
    /**
     * Validate the given model content.
     *
     * @param modelContent      the model content to validate
     * @param validationContext the validation context
     * @return
     */
    default Collection<ModelValidationError> validateModelContent(
        byte[] modelContent,
        ValidationContext validationContext
    ) {
        return validate(modelContent, validationContext);
    }

    /**
     * Validate the given model content and it's usage
     *
     * @param model             the model to validate
     * @param modelContent      the model content to validate
     * @param validationContext the validation context
     * @param validateUsage validate the usage of the model
     */
    default Collection<ModelValidationError> validateModelContent(
        Model model,
        byte[] modelContent,
        ValidationContext validationContext,
        boolean validateUsage
    ) {
        return validate(model, modelContent, validationContext, validateUsage);
    }
}
