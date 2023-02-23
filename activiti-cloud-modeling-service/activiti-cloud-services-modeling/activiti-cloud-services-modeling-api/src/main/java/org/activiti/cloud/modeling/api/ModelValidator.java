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
import org.activiti.cloud.api.error.ModelingException;

/**
 * Business logic related with validation of a model
 */
public interface ModelValidator extends ModelValidationErrorProducer {
    /**
     * Validate the given model.
     *
     * @param modelContent      content of the model to validate
     * @param validationContext the validation context
     */
    void validate(byte[] modelContent, ValidationContext validationContext);

    /**
     * Validate the given model and return exceptions
     *
     * @param modelContent      content of the model to validate
     * @param validationContext the validation context
     */
    Collection<ModelingException> validateAndReturnErrors(byte[] modelContent, ValidationContext validationContext);

  /**
   * Validate the given model and return exceptions
   *
   * @param modelContent      content of the model to validate
   * @param validationContext the validation context
   */
  Collection<ModelValidationError> validateAndReturnErrors(byte[] modelContent,
                                                        ValidationContext validationContext);

    /**
     * Get handled model type by this validator.
     *
     * @return handled model type
     */
    ModelType getHandledModelType();

    /**
     * Get handled content type by this validator.
     *
     * @return handled content type
     */
    String getHandledContentType();
}
