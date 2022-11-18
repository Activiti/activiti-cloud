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
package org.activiti.cloud.services.modeling.validation.process;

import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;
import org.activiti.cloud.modeling.api.ValidationContext;

/**
 * Interface for validating {@link BpmnModel} objects
 */
public interface BpmnCommonModelValidator extends ModelValidationErrorProducer {
    Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext);
}
