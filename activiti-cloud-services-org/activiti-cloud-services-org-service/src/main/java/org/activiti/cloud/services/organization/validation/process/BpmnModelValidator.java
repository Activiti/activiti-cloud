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

package org.activiti.cloud.services.organization.validation.process;

import java.util.stream.Stream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ModelValidationErrorProducer;
import org.activiti.cloud.organization.api.ValidationContext;

/**
 * Interface for validating {@link BpmnModel} objects
 */
public interface BpmnModelValidator extends ModelValidationErrorProducer {

    Stream<ModelValidationError> validate(BpmnModel bpmnModel,
                                          ValidationContext validationContext);

    default <T extends Task> Stream<T> getTasks(BpmnModel bpmnModel,
                                                Class<T> taskType) {
        return bpmnModel.getProcesses()
                .stream()
                .flatMap(process -> process.getFlowElements().stream())
                .filter(element -> taskType.isAssignableFrom(element.getClass()))
                .map(taskType::cast);
    }
}
