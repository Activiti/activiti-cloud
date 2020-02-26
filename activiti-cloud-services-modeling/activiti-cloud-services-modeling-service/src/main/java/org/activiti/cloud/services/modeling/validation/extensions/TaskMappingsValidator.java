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

package org.activiti.cloud.services.modeling.validation.extensions;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.Constant;

/**
 * Task mappings validator interface.
 * This implements specific process tasks mapping validations
 * and is related to the more generic process extensions validation logic
 * implemented in {@link ModelExtensionsValidator}
 */
public interface TaskMappingsValidator extends ModelValidationErrorProducer {

    /**
     * Validate the given list of task mappings.
     * @param taskMappings the list of task mappings to validate
     * @param validationContext the validation context
     * @param taskConstants the constants associated to the task
     * @return the stream of validation errors
     */
    Stream<ModelValidationError> validateTaskMappings(List<TaskMapping> taskMappings,
                                                      Map<String, Constant> taskConstants,
                                                      ValidationContext validationContext);
}
