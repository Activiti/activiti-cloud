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
import static org.activiti.cloud.modeling.api.process.ServiceTaskActionType.INPUTS;
import static org.activiti.cloud.modeling.api.process.ServiceTaskActionType.OUTPUTS;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.Constant;
import org.activiti.cloud.modeling.api.process.ProcessVariableMapping;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;
import org.activiti.cloud.modeling.api.process.VariableMappingType;
import org.activiti.cloud.services.modeling.converter.ConnectorActionParameter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelFeature;
import org.activiti.cloud.services.modeling.validation.process.ServiceTaskImplementationType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link TaskMappingsValidator} for {@link ServiceTask} implementation
 */
public class TaskMappingsServiceTaskImplementationValidator implements TaskMappingsValidator {

    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM =
        "Unknown %s connector parameter name in process extensions: %s";
    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION =
        "The extensions for process '%s' contains mappings to task '%s' for an unknown %s connector parameter name '%s'";

    public static final String UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_PROBLEM =
        "Unknown %s mapping connector action referenced in task %s: '%s'";
    public static final String UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_DESCRIPTION =
        "The extensions for process '%s' contains %s mappings to task '%s' referencing an unknown connector action '%s'";

    private final ConnectorModelType connectorModelType;

    private final ConnectorModelContentConverter connectorModelContentConverter;

    public TaskMappingsServiceTaskImplementationValidator(
        ConnectorModelType connectorModelType,
        ConnectorModelContentConverter connectorModelContentConverter
    ) {
        this.connectorModelType = connectorModelType;
        this.connectorModelContentConverter = connectorModelContentConverter;
    }

    @Override
    public Stream<ModelValidationError> validateTaskMappings(
        List<MappingModel> taskMappings,
        Map<String, Constant> taskConstants,
        ValidationContext validationContext
    ) {
        Map<String, ConnectorModelFeature> availableConnectorActions = getAvailableConnectorActions(validationContext);

        return taskMappings
            .stream()
            .flatMap(taskMapping -> validateTaskMapping(taskMapping, availableConnectorActions));
    }

    private Stream<ModelValidationError> validateTaskMapping(
        MappingModel taskMapping,
        Map<String, ConnectorModelFeature> availableConnectorActions
    ) {
        Optional<String> implementationTask = getTaskImplementation(taskMapping.getFlowNode());

        if (
            implementationTask.isPresent() &&
            isConnector(implementationTask) &&
            !availableConnectorActions.containsKey(implementationTask.get())
        ) {
            return Optional
                .of(
                    new ModelValidationError(
                        format(
                            UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_PROBLEM,
                            taskMapping.getAction().name(),
                            taskMapping.getFlowNode().getId(),
                            implementationTask.get()
                        ),
                        format(
                            UNKNOWN_CONNECTOR_ACTION_VALIDATION_ERROR_DESCRIPTION,
                            taskMapping.getProcessId(),
                            taskMapping.getAction().name(),
                            taskMapping.getFlowNode().getId(),
                            implementationTask.get()
                        )
                    )
                )
                .stream();
        }

        return taskMapping
            .getProcessVariableMappings()
            .entrySet()
            .stream()
            .map(variableMappingEntry ->
                validateTaskMappings(
                    taskMapping.getProcessId(),
                    taskMapping.getFlowNode(),
                    taskMapping.getAction(),
                    variableMappingEntry.getKey(),
                    variableMappingEntry.getValue(),
                    availableConnectorActions
                )
            )
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private boolean isConnector(Optional<String> implementationTask) {
        return (
            implementationTask.isPresent() &&
            !Arrays
                .stream(ServiceTaskImplementationType.values())
                .anyMatch(serviceImplementation ->
                    implementationTask.get().startsWith(serviceImplementation.getPrefix())
                )
        );
    }

    private Optional<ModelValidationError> validateTaskMappings(
        String processId,
        FlowNode task,
        ServiceTaskActionType actionType,
        String processVariableMappingKey,
        ProcessVariableMapping processVariableMapping,
        Map<String, ConnectorModelFeature> availableConnectorActions
    ) {
        if (actionType == OUTPUTS && processVariableMapping.getType() == VariableMappingType.VALUE) {
            return Optional.<ModelValidationError>empty();
        } else {
            Object connectorParameterName = actionType == INPUTS
                ? processVariableMappingKey
                : processVariableMapping.getValue();

            return getTaskImplementation(task)
                .map(availableConnectorActions::get)
                .flatMap(action ->
                    Optional
                        .ofNullable(actionType == INPUTS ? action.getInputs() : action.getOutputs())
                        .map(Arrays::stream)
                        .orElseGet(Stream::empty)
                        .map(ConnectorActionParameter::getName)
                        .filter(parameter -> parameter.equals(connectorParameterName))
                        .findFirst()
                        .map(parameter -> Optional.<ModelValidationError>empty())
                        .orElseGet(() ->
                            Optional.of(
                                new ModelValidationError(
                                    format(
                                        UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM,
                                        actionType.name().toLowerCase(),
                                        connectorParameterName
                                    ),
                                    format(
                                        UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION,
                                        processId,
                                        task.getId(),
                                        actionType.name().toLowerCase(),
                                        connectorParameterName
                                    )
                                )
                            )
                        )
                );
        }
    }

    private Optional<String> getTaskImplementation(FlowNode task) {
        return Optional
            .ofNullable(task)
            .filter(t -> ServiceTask.class.isAssignableFrom(t.getClass()))
            .map(ServiceTask.class::cast)
            .map(ServiceTask::getImplementation);
    }

    private Map<String, ConnectorModelFeature> getAvailableConnectorActions(ValidationContext validationContext) {
        Map<String, ConnectorModelFeature> availableConnectorActions = new HashMap<>();
        validationContext
            .getAvailableModels(connectorModelType)
            .stream()
            .map(Model::getContent)
            .map(connectorModelContentConverter::convertToModelContent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(connectorModelContent -> !CollectionUtils.isEmpty(connectorModelContent.getActions()))
            .forEach(connectorModelContent ->
                connectorModelContent
                    .getActions()
                    .values()
                    .forEach(action ->
                        availableConnectorActions.put(
                            getImplementationKey(connectorModelContent.getKey(), action),
                            action
                        )
                    )
            );

        return availableConnectorActions;
    }

    private String getImplementationKey(String connectorKey, ConnectorModelFeature action) {
        return ObjectUtils.isEmpty(action) && ObjectUtils.isEmpty(action.getName())
            ? connectorKey
            : String.join(".", connectorKey, action.getName());
    }
}
