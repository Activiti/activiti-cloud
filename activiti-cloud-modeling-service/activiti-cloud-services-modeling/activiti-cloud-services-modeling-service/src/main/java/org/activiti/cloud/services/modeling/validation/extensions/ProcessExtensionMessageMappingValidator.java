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
package org.activiti.cloud.services.modeling.validation.extensions;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.Event;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.ThrowEvent;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.api.process.ProcessVariableMapping;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;
import org.activiti.cloud.services.modeling.converter.BpmnProcessModelContent;

/**
 * Implementation of {@link ProcessExtensionsValidator} for validating message payload name
 */
public class ProcessExtensionMessageMappingValidator implements ProcessExtensionsValidator {

    private static final String PAYLOAD_PATTERN = "[a-z]([a-zA-Z0-9]+)?";
    private static final String INVALID_PAYLOAD_NAME_ERROR =
        "Invalid %s message payload name in process extensions: %s";
    private static final String INVALID_PAYLOAD_NAME_ERROR_DESCRIPTION =
        "The extensions for process '%s' contains mappings to element '%s' for an invalid payload name '%s'";

    @Override
    public Stream<ModelValidationError> validateExtensions(
        Extensions extensions,
        BpmnProcessModelContent bpmnModel,
        ValidationContext validationContext
    ) {
        Set<FlowNode> messageElements = bpmnModel
            .findAllNodes(
                StartEvent.class,
                IntermediateCatchEvent.class,
                BoundaryEvent.class,
                ThrowEvent.class,
                EndEvent.class
            )
            .stream()
            .filter(element -> isMessageElement((Event) element))
            .collect(Collectors.toSet());

        return extensions
            .getVariablesMappings()
            .entrySet()
            .stream()
            .flatMap(taskMapping ->
                validateMapping(
                    bpmnModel.getId(),
                    taskMapping.getKey(),
                    taskMapping.getValue().getServiceTaskActionTypeMap(),
                    messageElements
                )
            );
    }

    private Stream<ModelValidationError> validateMapping(
        String processId,
        String elementId,
        Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> extensionMapping,
        Set<FlowNode> messages
    ) {
        FlowNode flowNode = messages
            .stream()
            .filter(message -> message.getId().equals(elementId))
            .findFirst()
            .orElse(null);

        Stream<ModelValidationError> errorStream = Stream.of();
        if (flowNode != null) {
            errorStream =
                extensionMapping
                    .entrySet()
                    .stream()
                    .map(mappingEntry ->
                        new MappingModel(processId, flowNode, mappingEntry.getKey(), mappingEntry.getValue())
                    )
                    .filter(mappingModel -> mappingModel.getAction() == ServiceTaskActionType.INPUTS)
                    .map(this::validate)
                    .flatMap(Collection::stream);
        }

        return errorStream;
    }

    private List<ModelValidationError> validate(MappingModel mappingModel) {
        return mappingModel
            .getProcessVariableMappings()
            .entrySet()
            .stream()
            .filter(variable -> !variable.getKey().matches(PAYLOAD_PATTERN))
            .map(variable ->
                new ModelValidationError(
                    format(
                        INVALID_PAYLOAD_NAME_ERROR,
                        mappingModel.getFlowNode().getName(),
                        mappingModel.getProcessId()
                    ),
                    format(
                        INVALID_PAYLOAD_NAME_ERROR_DESCRIPTION,
                        mappingModel.getProcessId(),
                        mappingModel.getFlowNode().getId(),
                        variable.getKey()
                    )
                )
            )
            .collect(Collectors.toList());
    }

    private boolean isMessageElement(Event element) {
        if (element.getEventDefinitions() != null && element.getEventDefinitions().size() > 0) {
            return (
                element
                    .getEventDefinitions()
                    .stream()
                    .filter(definition -> definition instanceof MessageEventDefinition)
                    .toArray()
                    .length >
                0
            );
        }
        return false;
    }
}
