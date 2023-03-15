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

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;

/**
 * Implementation of {@link BpmnCommonModelValidator} for validating service task boundaries
 */
public class BpmnModelServiceTaskCatchBoundaryValidator implements BpmnCommonModelValidator {

    public static final String MISSING_BOUNDARY_WARNING = "Missing Catch Error boundary event";
    public static final String INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION =
        "The service implementation on service '%s' might fail silently. " +
        "Consider adding an Error boundary event to handle failures.";
    public static final String SERVICE_TASK_VALIDATOR_NAME = "BPMN service task catch boundary validator";

    private final FlowElementsExtractor flowElementsExtractor;

    private final List<ServiceTaskImplementationType> serviceTaskImplementationTypes;

    public BpmnModelServiceTaskCatchBoundaryValidator(
        FlowElementsExtractor flowElementsExtractor,
        List<ServiceTaskImplementationType> serviceTaskImplementationTypes
    ) {
        this.flowElementsExtractor = flowElementsExtractor;
        this.serviceTaskImplementationTypes =
            Optional.ofNullable(serviceTaskImplementationTypes).orElse(Collections.emptyList());
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel, ValidationContext validationContext) {
        return flowElementsExtractor
            .extractFlowElements(bpmnModel, ServiceTask.class)
            .stream()
            .filter(serviceTask -> serviceTask.getImplementation() != null)
            .map(serviceTask -> validateServiceTaskBoundary(serviceTask))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Optional<ModelValidationError> validateServiceTaskBoundary(ServiceTask serviceTask) {
        if (requiredBoundaryIsMissing(serviceTask)) {
            return Optional.of(
                new ModelValidationError(
                    MISSING_BOUNDARY_WARNING,
                    format(INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION, serviceTask.getId()),
                    SERVICE_TASK_VALIDATOR_NAME,
                    true
                )
            );
        }

        return Optional.<ModelValidationError>empty();
    }

    private boolean requiredBoundaryIsMissing(ServiceTask serviceTask) {
        return serviceTaskImplementationTypes
            .stream()
            .anyMatch(serviceImplementation ->
                serviceTask.getImplementation().startsWith(serviceImplementation.getPrefix()) &&
                !serviceTask.hasBoundaryErrorEvents()
            );
    }
}
