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

import org.activiti.bpmn.model.FlowNode;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.api.process.ProcessVariableMapping;
import org.activiti.cloud.organization.api.process.ServiceTaskActionType;
import org.activiti.cloud.services.organization.converter.BpmnProcessModelContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link ProcessExtensionsValidator} for validating task mappings
 */
public class ProcessExtensionsTaskMappingsValidator implements ProcessExtensionsValidator {

    public static final String UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM = "Unknown task in process extensions: %s";
    public static final String UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION = "The extensions for process '%s' contains mappings for an unknown task '%s'";

    private final Set<TaskMappingsValidator> taskMappingsValidators;

    public ProcessExtensionsTaskMappingsValidator(Set<TaskMappingsValidator> taskMappingsValidators) {
        this.taskMappingsValidators = taskMappingsValidators;
    }

    @Override
    public Stream<ModelValidationError> validate(Extensions extensions,
                                                 BpmnProcessModelContent bpmnModel,
                                                 ValidationContext validationContext) {
        Set<FlowNode> availableTasks = bpmnModel.findAllNodes();
        return extensions.getVariablesMappings().entrySet()
                .stream()
                .flatMap(taskMapping -> validateTaskMapping(bpmnModel.getId(),
                                                            taskMapping.getKey(),
                                                            taskMapping.getValue(),
                                                            availableTasks,
                                                            validationContext));
    }

    private Stream<ModelValidationError> validateTaskMapping(String processId,
                                                             String taskId,
                                                             Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> extensionMapping,
                                                             Set<FlowNode> availableTasks,
                                                             ValidationContext context) {
        return availableTasks
                .stream()
                .filter(task -> Objects.equals(task.getId(),
                                               taskId))
                .findFirst()
                .map(task -> validateTaskMappings(processId,
                                                  task,
                                                  extensionMapping,
                                                  context))
                .orElseGet(() -> Stream.of(createModelValidationError(
                        format(UNKNOWN_TASK_VALIDATION_ERROR_PROBLEM,
                               taskId),
                        format(UNKNOWN_TASK_VALIDATION_ERROR_DESCRIPTION,
                               processId,
                               taskId))));
    }

    private Stream<ModelValidationError> validateTaskMappings(
            String processId,
            FlowNode task,
            Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> taskMappingsMap,
            ValidationContext validationContext) {

        List<TaskMapping> taskMappings = toTaskMappings(processId,
                                                        task,
                                                        taskMappingsMap);
        return taskMappingsValidators
                .stream()
                .flatMap(validator -> validator.validateTaskMappings(taskMappings,
                                                                     validationContext));
    }

    private List<TaskMapping> toTaskMappings(String processId,
                                              FlowNode task,
                                              Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> taskMappingsMap) {
        return taskMappingsMap.entrySet()
                .stream()
                .map(taskMappingEntry -> new TaskMapping(processId,
                                                         task,
                                                         taskMappingEntry.getKey(),
                                                         taskMappingEntry.getValue()))
                .collect(Collectors.toList());
    }
}
