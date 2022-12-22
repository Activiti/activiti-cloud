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
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ErrorEventDefinition;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelFeature;

/**
 * Implementation of {@link BpmnCommonModelValidator} for validating service task boundaries
 */
public class BpmnModelServiceTaskCatchBoundaryValidator implements BpmnCommonModelValidator {

    public static final String MISSING_BOUNDARY_WARNING = "Missing Catch Error boundary event";
    public static final String INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION = "The service implementation on service '%s' might fail silently";
    public static final String SERVICE_TASK_VALIDATOR_NAME = "BPMN service task catch boundary validator";

    private final ConnectorModelType connectorModelType;

    private final ConnectorModelContentConverter connectorModelContentConverter;

    private final FlowElementsExtractor flowElementsExtractor;

    private final ServiceTaskImplementationType[] serviceTaskImplementationTypes;

    public BpmnModelServiceTaskCatchBoundaryValidator(ConnectorModelType connectorModelType,
        ConnectorModelContentConverter connectorModelContentConverter,
        FlowElementsExtractor flowElementsExtractor,
        ServiceTaskImplementationType[] serviceTaskImplementationTypes) {
        this.connectorModelType = connectorModelType;
        this.connectorModelContentConverter = connectorModelContentConverter;
        this.flowElementsExtractor = flowElementsExtractor;
        this.serviceTaskImplementationTypes = Optional.ofNullable(serviceTaskImplementationTypes)
            .orElse(new ServiceTaskImplementationType[0]);
    }

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
        ValidationContext validationContext) {

        return flowElementsExtractor.extractFlowElements(bpmnModel, ServiceTask.class).stream()
            .filter(serviceTask -> serviceTask.getImplementation() != null)
            .map(serviceTask -> validateServiceTaskBoundary(serviceTask))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private String concatNameAndAction(ConnectorModelFeature connectorModelFeature,
        Model model) {
        return isEmpty(connectorModelFeature) && isEmpty(connectorModelFeature.getName()) ?
            model.getName() :
            model.getName() + "." + connectorModelFeature.getName();
    }

    private Optional<ModelValidationError> validateServiceTaskBoundary(ServiceTask serviceTask) {
        if (requiresBoundary(serviceTask)) {
            return Optional.of(
                new ModelValidationError(MISSING_BOUNDARY_WARNING,
                    format(INVALID_SERVICE_IMPLEMENTATION_DESCRIPTION,
                        serviceTask.getId()), SERVICE_TASK_VALIDATOR_NAME, true));
        }

        return Optional.<ModelValidationError>empty();
    }

    private boolean requiresBoundary(ServiceTask serviceTask) {
        return Arrays.stream(serviceTaskImplementationTypes)
            .anyMatch(serviceImplementation ->
                serviceTask.getImplementation().startsWith(serviceImplementation.getPrefix()) &&
                    (
                        serviceTask.getBoundaryEvents() == null ||
                            !hasBoundaryErrorEvent(serviceTask.getBoundaryEvents())
                    )
            );
    }

    private boolean hasBoundaryErrorEvent(List<BoundaryEvent> boundaryEvents) {
        return boundaryEvents.stream().anyMatch(boundaryEvent ->
            boundaryEvent.getEventDefinitions().stream().anyMatch(eventDefinition ->
                ErrorEventDefinition.class.isInstance(eventDefinition)
            )
        );
    }
}
