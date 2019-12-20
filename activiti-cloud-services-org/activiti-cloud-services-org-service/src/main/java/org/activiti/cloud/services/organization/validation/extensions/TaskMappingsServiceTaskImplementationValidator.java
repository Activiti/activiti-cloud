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

package org.activiti.cloud.services.organization.validation.extensions;

import static java.lang.String.format;
import static org.activiti.cloud.organization.api.process.ServiceTaskActionType.INPUTS;
import static org.activiti.cloud.organization.api.process.ServiceTaskActionType.OUTPUTS;
import static org.springframework.util.StringUtils.isEmpty;

import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.api.process.Constant;
import org.activiti.cloud.organization.api.process.ProcessVariableMapping;
import org.activiti.cloud.organization.api.process.ServiceTaskActionType;
import org.activiti.cloud.organization.api.process.VariableMappingType;
import org.activiti.cloud.services.organization.converter.ConnectorActionParameter;
import org.activiti.cloud.services.organization.converter.ConnectorModelAction;
import org.activiti.cloud.services.organization.converter.ConnectorModelContentConverter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of {@link TaskMappingsValidator} for {@link ServiceTask} implementation
 */
public class TaskMappingsServiceTaskImplementationValidator implements TaskMappingsValidator {

    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM =
            "Unknown %s connector parameter name in process extensions: %s";
    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION =
            "The extensions for process '%s' contains mappings to task '%s' for an unknown %s connector parameter name '%s'";

    private final ConnectorModelType connectorModelType;

    private final ConnectorModelContentConverter connectorModelContentConverter;

    public TaskMappingsServiceTaskImplementationValidator(ConnectorModelType connectorModelType,
                                                          ConnectorModelContentConverter connectorModelContentConverter) {
        this.connectorModelType = connectorModelType;
        this.connectorModelContentConverter = connectorModelContentConverter;
    }

    @Override
    public Stream<ModelValidationError> validateTaskMappings(List<TaskMapping> taskMappings,
                                                             Map<String, Constant> taskConstants,
                                                             ValidationContext validationContext) {
        Map<String, ConnectorModelAction> availableConnectorActions = getAvailableConnectorActions(validationContext);
        return taskMappings
                .stream()
                .flatMap(taskMapping -> validateTaskMapping(taskMapping,
                                                            availableConnectorActions));
    }

    private Stream<ModelValidationError> validateTaskMapping(TaskMapping taskMapping,
                                                            Map<String, ConnectorModelAction> availableConnectorActions) {
        return taskMapping
                .getProcessVariableMappings()
                .entrySet()
                .stream()
                .map(variableMappingEntry -> validateTaskMappings(taskMapping.getProcessId(),
                                                                  taskMapping.getTask(),
                                                                  taskMapping.getAction(),
                                                                  variableMappingEntry.getKey(),
                                                                  variableMappingEntry.getValue(),
                                                                  availableConnectorActions))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<ModelValidationError> validateTaskMappings(String processId,
                                                                FlowNode task,
                                                                ServiceTaskActionType actionType,
                                                                String processVariableMappingKey,
                                                                ProcessVariableMapping processVariableMapping,
                                                                Map<String, ConnectorModelAction> availableConnectorActions) {

        if(actionType == OUTPUTS && processVariableMapping.getType() == VariableMappingType.VALUE) {
            return Optional.<ModelValidationError>empty();
        }else {
            Object connectorParameterName = actionType == INPUTS ? processVariableMappingKey : processVariableMapping.getValue();
            return getTaskImplementation(task)
                    .map(availableConnectorActions::get)
                    .flatMap(action -> Optional.ofNullable(actionType == INPUTS ? action.getInputs() : action.getOutputs())
                            .map(Arrays::stream)
                            .orElseGet(Stream::empty)
                            .map(ConnectorActionParameter::getName)
                            .filter(parameter -> parameter.equals(connectorParameterName))
                            .findFirst()
                            .map(parameter -> Optional.<ModelValidationError>empty())
                            .orElseGet(() -> Optional.of(createModelValidationError(
                                    format(UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM,
                                           actionType.name().toLowerCase(),
                                           connectorParameterName),
                                    format(UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION,
                                           processId,
                                           task.getId(),
                                           actionType.name().toLowerCase(),
                                           connectorParameterName)))));
        }
    }

    private Optional<String> getTaskImplementation(FlowNode task) {
        return Optional.ofNullable(task)
                .filter(t -> ServiceTask.class.isAssignableFrom(t.getClass()))
                .map(ServiceTask.class::cast)
                .map(ServiceTask::getImplementation);
    }

    private Map<String, ConnectorModelAction> getAvailableConnectorActions(ValidationContext validationContext) {
        Map<String, ConnectorModelAction> availableConnectorActions = new HashMap<>();
        validationContext.getAvailableModels(connectorModelType)
                .stream()
                .map(Model::getContent)
                .map(connectorModelContentConverter::convertToModelContent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(connectorModelContent -> connectorModelContent.getActions().values()
                        .forEach(action -> availableConnectorActions.put(getImplementationKey(connectorModelContent.getName(),
                                                                                              action),
                                                                         action)));

        return availableConnectorActions;
    }

    private String getImplementationKey(String connectorName,
                                        ConnectorModelAction action) {
        return isEmpty(action) && isEmpty(action.getName()) ?
                connectorName :
                String.join(".",
                            connectorName,
                            action.getName());
    }
}
