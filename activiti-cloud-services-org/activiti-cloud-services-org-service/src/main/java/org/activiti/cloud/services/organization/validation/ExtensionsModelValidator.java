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
package org.activiti.cloud.services.organization.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.api.process.ProcessVariable;
import org.activiti.cloud.organization.api.process.ProcessVariableMapping;
import org.activiti.cloud.organization.api.process.ServiceTaskActionType;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.core.error.SyntacticModelValidationException;
import org.activiti.cloud.services.organization.converter.BpmnProcessModelContent;
import org.activiti.cloud.services.organization.converter.ConnectorActionParameter;
import org.activiti.cloud.services.organization.converter.ConnectorModelAction;
import org.activiti.cloud.services.organization.converter.ConnectorModelContentConverter;
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.activiti.cloud.organization.api.process.ServiceTaskActionType.INPUTS;
import static org.activiti.cloud.organization.api.process.VariableMappingType.VARIABLE;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.apache.commons.lang3.StringUtils.removeStart;

@Component
@ConditionalOnMissingBean(name = "ExtensionsModelValidator")
public class ExtensionsModelValidator extends JsonSchemaModelValidator {

    public static final String UNKNOWN_PROCESS_ID_VALIDATION_ERROR_PROBLEM = "Unknown process id in process extensions: %s";
    public static final String UNKNOWN_PROCESS_ID_VALIDATION_ERROR_DESCRIPTION = "The process extensions are bound to an unknown process id '%s'";
    public static final String UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM = "Unknown task in process extensions: %s";
    public static final String UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown task '%s'";
    public static final String UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_PROBLEM = "Unknown process variable in process extensions: %s";
    public static final String UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown process variable '%s'";
    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM = "Unknown %s connector parameter name in process extensions: %s";
    public static final String UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown %s connector parameter name '%s'";

    private final SchemaLoader processExtensionsSchemaLoader;

    private final ProcessModelType processModelType;

    private final JsonConverter<Model> extensionsConverter;

    private final ProcessModelContentConverter processModelContentConverter;

    private final ConnectorModelContentConverter connectorModelContentConverter;

    @Autowired
    public ExtensionsModelValidator(SchemaLoader processExtensionsSchemaLoader,
                                    ProcessModelType processModelType,
                                    JsonConverter<Model> extensionsConverter,
                                    ProcessModelContentConverter processModelContentConverter,
                                    ConnectorModelContentConverter connectorModelContentConverter) {
        this.processExtensionsSchemaLoader = processExtensionsSchemaLoader;
        this.processModelType = processModelType;
        this.extensionsConverter = extensionsConverter;
        this.processModelContentConverter = processModelContentConverter;
        this.connectorModelContentConverter = connectorModelContentConverter;
    }

    @Override
    public void validateModelContent(byte[] bytes,
                                     ValidationContext validationContext) {
        super.validateModelContent(bytes,
                                   validationContext);

        validateModelContentInContext(bytes,
                                      validationContext);
    }

    protected void validateModelContentInContext(byte[] bytes,
                                                 ValidationContext validationContext) {
        List<ModelValidationError> validationExceptions =
                validateModelExtensions(convertBytesToModel(bytes),
                                        validationContext)
                        .collect(Collectors.toList());
        if (!validationExceptions.isEmpty()) {
            throw new SemanticModelValidationException(
                    "Semantic model validation errors encountered: " + validationExceptions
                            .stream()
                            .map(ModelValidationError::getDescription)
                            .collect(Collectors.joining(",")),
                    validationExceptions);
        }
    }

    protected Stream<ModelValidationError> validateModelExtensions(Model model,
                                                                   ValidationContext context) {
        return Optional.ofNullable(model.getId())
                .map(modelId -> removeStart(modelId,
                                            ProcessModelType.PROCESS.toLowerCase() + "-"))
                .flatMap(modelId -> findModelInContext(modelId,
                                                       context))
                .map(Model::getContent)
                .map(String::getBytes)
                .flatMap(this::convertToBpmnModel)
                .map(bpmnModel -> validateModelExtensions(model,
                                                          getAvailableConnectorActions(context),
                                                          bpmnModel))
                .orElseGet(() -> Stream.of(createModelValidationError(
                        format(UNKNOWN_PROCESS_ID_VALIDATION_ERROR_PROBLEM,
                               model.getId()),
                        format(UNKNOWN_PROCESS_ID_VALIDATION_ERROR_DESCRIPTION,
                               model.getId()))));
    }

    protected Stream<ModelValidationError> validateModelExtensions(Model model,
                                                                   Map<String, ConnectorModelAction> availableConnectorActions,
                                                                   BpmnProcessModelContent bpmnModel) {
        return Optional.ofNullable(model.getExtensions())
                .map(extensions -> validateModelExtensions(extensions,
                                                           availableConnectorActions,
                                                           bpmnModel))
                .orElseGet(Stream::empty);
    }

    protected Stream<ModelValidationError> validateModelExtensions(Extensions extensions,
                                                                   Map<String, ConnectorModelAction> availableConnectorActions,
                                                                   BpmnProcessModelContent bpmnModel) {
        Set<String> availableProcessVariables = getAvailableProcessVariables(extensions);
        Set<FlowNode> availableNodes = bpmnModel.findAllNodes();

        return Stream.concat(
                validateTaskMappings(extensions,
                                     bpmnModel.getId(),
                                     availableNodes,
                                     availableConnectorActions),
                validateVariableMappings(extensions,
                                         bpmnModel.getId(),
                                         availableProcessVariables)
        );
    }

    private Set<String> getAvailableProcessVariables(Extensions extensions) {
        return extensions
                .getProcessVariables()
                .values()
                .stream()
                .map(ProcessVariable::getName)
                .collect(Collectors.toSet());
    }

    private Stream<ModelValidationError> validateTaskMappings(Extensions extensions,
                                                              String modelId,
                                                              Set<FlowNode> availableNodes,
                                                              Map<String, ConnectorModelAction> availableConnectorActions) {
        return extensions.getVariablesMappings().entrySet()
                .stream()
                .flatMap(taskMapping -> validateTaskMapping(taskMapping.getKey(),
                                                            taskMapping.getValue(),
                                                            modelId,
                                                            availableNodes,
                                                            availableConnectorActions));
    }

    private Stream<ModelValidationError> validateVariableMappings(Extensions extensions,
                                                                  String modelId,
                                                                  Set<String> availableProcessVariables) {
        return extensions.getVariablesMappings().values()
                .stream()
                .flatMap(actionMappings -> actionMappings.entrySet().stream())
                .flatMap(taskMappingEntry -> validateProcessVariableMapping(taskMappingEntry.getKey(),
                                                                            taskMappingEntry.getValue(),
                                                                            modelId,
                                                                            availableProcessVariables));
    }

    private Stream<ModelValidationError> validateProcessVariableMapping(ServiceTaskActionType action,
                                                                        Map<String, ProcessVariableMapping> processVariableMappings,
                                                                        String modelId,
                                                                        Set<String> availableProcessVariables) {
        return processVariableMappings.entrySet().stream()
                .map(valiableMappingEntry -> validateProcessVariableMapping(action,
                                                                            valiableMappingEntry.getKey(),
                                                                            valiableMappingEntry.getValue(),
                                                                            modelId,
                                                                            availableProcessVariables))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Stream<ModelValidationError> validateTaskMapping(String taskId,
                                                             Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> extensionMapping,
                                                             String modelId,
                                                             Set<FlowNode> availableNodes,
                                                             Map<String, ConnectorModelAction> availableConnectorActions) {
        return availableNodes
                .stream()
                .filter(node -> Objects.equals(node.getId(),
                                               taskId))
                .findFirst()
                .map(activity -> validateConnectorParameter(activity,
                                                            extensionMapping,
                                                            modelId,
                                                            availableConnectorActions))
                .orElseGet(() -> Stream.of(createModelValidationError(
                        format(UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM,
                               taskId),
                        format(UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION,
                               modelId,
                               taskId))));
    }

    private Stream<ModelValidationError> validateConnectorParameter(
            FlowNode node,
            Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> taskMapping,
            String modelId,
            Map<String, ConnectorModelAction> availableConnectorActions) {
        if (node instanceof ServiceTask) {
            return taskMapping.entrySet()
                    .stream()
                    .flatMap(taskMappingEntry -> validateConnectorParameter((ServiceTask) node,
                                                                            taskMappingEntry.getKey(),
                                                                            taskMappingEntry.getValue(),
                                                                            modelId,
                                                                            availableConnectorActions));
        }

        return Stream.empty();
    }

    private Stream<ModelValidationError> validateConnectorParameter(ServiceTask task,
                                                                    ServiceTaskActionType action,
                                                                    Map<String, ProcessVariableMapping> processVariableMappings,
                                                                    String modelId,
                                                                    Map<String, ConnectorModelAction> availableConnectorActions) {
        return processVariableMappings.entrySet()
                .stream()
                .map(variableMappingEntry -> validateTaskActionMapping(task,
                                                                       action,
                                                                       variableMappingEntry.getKey(),
                                                                       variableMappingEntry.getValue(),
                                                                       modelId,
                                                                       availableConnectorActions))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<ModelValidationError> validateTaskActionMapping(ServiceTask task,
                                                                     ServiceTaskActionType actionType,
                                                                     String processVariableMappingKey,
                                                                     ProcessVariableMapping processVariableMapping,
                                                                     String modelId,
                                                                     Map<String, ConnectorModelAction> availableConnectorActions) {

        String connectorParameterName = actionType == INPUTS ? processVariableMappingKey : processVariableMapping.getValue();
        return Optional.ofNullable(availableConnectorActions.get(task.getImplementation()))
                .flatMap(action -> Arrays.stream(actionType == INPUTS ? action.getInputs() : action.getOutputs())
                        .map(ConnectorActionParameter::getName)
                        .filter(parameter -> parameter.equals(connectorParameterName))
                        .findFirst()
                        .map(parameter -> Optional.<ModelValidationError>empty())
                        .orElseGet(() -> Optional.of(createModelValidationError(
                                format(UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_PROBLEM,
                                       actionType.name().toLowerCase(),
                                       connectorParameterName),
                                format(UNKNOWN_CONNECTOR_PARAMETER_VALIDATION_ERROR_DESCRIPTION,
                                       modelId,
                                       actionType.name().toLowerCase(),
                                       connectorParameterName)))));
    }

    private Optional<ModelValidationError> validateProcessVariableMapping(ServiceTaskActionType action,
                                                                          String processVariableMappingKey,
                                                                          ProcessVariableMapping processVariableMapping,
                                                                          String modelId,
                                                                          Set<String> availableProcessVariables) {
        String variableName = action == INPUTS ? processVariableMapping.getValue() : processVariableMappingKey;
        return processVariableMapping.getType() == VARIABLE &&
                !availableProcessVariables.contains(variableName) ?
                Optional.of(createModelValidationError(
                        format(UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_PROBLEM,
                               variableName),
                        format(UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_DESCRIPTION,
                               modelId,
                               variableName))) :
                Optional.empty();
    }

    private Map<String, ConnectorModelAction> getAvailableConnectorActions(ValidationContext context) {
        Map<String, ConnectorModelAction> availableConnectorActions = new HashMap<>();
        context.getAvailableModels()
                .stream()
                .filter(modelInContext -> ConnectorModelType.NAME.equals(modelInContext.getType()))
                .map(Model::getContent)
                .map(String::getBytes)
                .map(connectorModelContentConverter::convertToModelContent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(connectorModelContent -> connectorModelContent.getActions().values()
                        .forEach(action -> availableConnectorActions.put(getImplementationKey(connectorModelContent.getName(),
                                                                                              action.getName()),
                                                                         action)));

        return availableConnectorActions;
    }

    private String getImplementationKey(String connectorName,
                                        String actionName) {
        return String.join(".",
                           connectorName,
                           actionName);
    }

    private Optional<Model> findModelInContext(String modelId,
                                               ValidationContext validationContext) {
        return validationContext.getAvailableModels()
                .stream()
                .filter(model -> Objects.equals(model.getId(),
                                                modelId))
                .findFirst();
    }

    private Model convertBytesToModel(byte[] bytes) {
        try {
            return extensionsConverter.convertToEntity(bytes);
        } catch (ModelingException ex) {
            throw new SyntacticModelValidationException("Cannot convert json extensions to a model",
                                                        ex);
        }
    }

    private Optional<BpmnProcessModelContent> convertToBpmnModel(byte[] bytes) {
        try {
            return processModelContentConverter.convertToModelContent(bytes);
        } catch (ModelingException ex) {
            throw new SyntacticModelValidationException("Cannot convert to BPMN model",
                                                        ex);
        }
    }

    @Override
    public ModelType getHandledModelType() {
        return processModelType;
    }

    @Override
    public String getHandledContentType() {
        return CONTENT_TYPE_JSON;
    }

    @Override
    public SchemaLoader schemaLoader() {
        return processExtensionsSchemaLoader;
    }
}
