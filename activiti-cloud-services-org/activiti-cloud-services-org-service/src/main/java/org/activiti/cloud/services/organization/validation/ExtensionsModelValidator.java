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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.activiti.cloud.services.organization.converter.ProcessModelContentConverter;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.activiti.cloud.organization.api.process.VariableMappingType.VARIABLE;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.apache.commons.lang3.StringUtils.removeStart;

@Component
public class ExtensionsModelValidator extends JsonSchemaModelValidator {

    public static final String UNKNOWN_PROCESS_ID_VALIDATION_ERROR_PROBLEM = "Unknown process id in process extensions: %s";
    public static final String UNKNOWN_PROCESS_ID_VALIDATION_ERROR_DESCRIPTION = "The process extensions are bound to an unknown process id '%s'";
    public static final String UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM = "Unknown task name in process extensions: %s";
    public static final String UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown task '%s'";
    public static final String UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_PROBLEM = "Unknown process variable in process extensions: %s";
    public static final String UNKNOWN_PROCESS_VARIABLE_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown process variable '%s'";

    private final SchemaLoader processExtensionsSchemaLoader;

    private final ProcessModelType processModelType;

    private final JsonConverter<Model> extensionsConverter;

    private final ProcessModelContentConverter processModelContentConverter;

    @Autowired
    public ExtensionsModelValidator(SchemaLoader processExtensionsSchemaLoader,
                                    ProcessModelType processModelType,
                                    JsonConverter<Model> extensionsConverter,
                                    ProcessModelContentConverter processModelContentConverter) {
        this.processExtensionsSchemaLoader = processExtensionsSchemaLoader;
        this.processModelType = processModelType;
        this.extensionsConverter = extensionsConverter;
        this.processModelContentConverter = processModelContentConverter;
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
                                                          bpmnModel))
                .orElseGet(() -> Stream.of(createModelValidationError(
                        format(UNKNOWN_PROCESS_ID_VALIDATION_ERROR_PROBLEM,
                               model.getId()),
                        format(UNKNOWN_PROCESS_ID_VALIDATION_ERROR_DESCRIPTION,
                               model.getId()))));
    }

    protected Stream<ModelValidationError> validateModelExtensions(Model model,
                                                                 BpmnProcessModelContent bpmnModel) {
        return Optional.ofNullable(model.getExtensions())
                .map(extensions -> validateModelExtensions(extensions,
                                                           bpmnModel))
                .orElseGet(Stream::empty);
    }

    protected Stream<ModelValidationError> validateModelExtensions(Extensions extensions,
                                                                 BpmnProcessModelContent bpmnModel) {
        Set<String> availableProcessVariables = getAvailableProcessVariables(extensions);
        return Stream.concat(
                validateTaskMappings(extensions,
                                     bpmnModel.getId(),
                                     bpmnModel.findAllTaskNames()),
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
                                                              Set<String> availableTasks) {
        return extensions.getVariablesMappings().keySet()
                .stream()
                .map(taskName -> validateTaskName(taskName,
                                                  modelId,
                                                  availableTasks))
                .filter(Optional::isPresent)
                .map(Optional::get);
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

    private Optional<ModelValidationError> validateTaskName(String taskName,
                                                            String modelId,
                                                            Set<String> availableTasks) {
        return !availableTasks.contains(taskName) ?
                Optional.of(createModelValidationError(
                        format(UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM,
                               taskName),
                        format(UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION,
                               modelId,
                               taskName))) :
                Optional.empty();
    }

    private Optional<ModelValidationError> validateProcessVariableMapping(ServiceTaskActionType action,
                                                                          String processVariableMappingKey,
                                                                          ProcessVariableMapping processVariableMapping,
                                                                          String modelId,
                                                                          Set<String> availableProcessVariables) {
        String variableName = action == ServiceTaskActionType.INPUTS ?
                processVariableMapping.getValue() :
                processVariableMappingKey;
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
