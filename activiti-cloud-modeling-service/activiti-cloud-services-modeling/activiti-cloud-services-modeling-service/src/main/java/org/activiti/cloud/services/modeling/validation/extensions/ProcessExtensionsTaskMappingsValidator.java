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

package org.activiti.cloud.services.modeling.validation.extensions;

import static java.lang.String.format;

import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.ThrowEvent;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.Constant;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.api.process.ProcessVariableMapping;
import org.activiti.cloud.modeling.api.process.ServiceTaskActionType;
import org.activiti.cloud.services.modeling.converter.BpmnProcessModelContent;
import org.springframework.stereotype.Component;

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
    public Stream<ModelValidationError> validateExtensions(Extensions extensions,
                                                           BpmnProcessModelContent bpmnModel,
                                                           ValidationContext validationContext) {

        Set<FlowNode> availableTasks = bpmnModel.findAllNodes(
                Task.class,
                CallActivity.class,
                StartEvent.class,
                IntermediateCatchEvent.class,
                EndEvent.class,
                BoundaryEvent.class,
                ThrowEvent.class);

        return extensions.getVariablesMappings().entrySet()
                .stream()
                .flatMap(taskMapping -> validateTaskMapping(bpmnModel.getId(),
                                                            taskMapping.getKey(),
                                                            taskMapping.getValue(),
                                                            getTaskConstants(extensions,taskMapping.getKey()),
                                                            availableTasks,
                                                            validationContext));
    }


    private Map<String, Constant> getTaskConstants(Extensions extensions,
                                                   String taskKey) {
        return extensions.getConstants() != null ? extensions.getConstants().get(taskKey) : null;
    }

    private Stream<ModelValidationError> validateTaskMapping(String processId,
                                                             String taskId,
                                                             Map<ServiceTaskActionType, Map<String, ProcessVariableMapping>> extensionMapping,
                                                             Map<String, Constant> taskConstants,
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
                                                  taskConstants,
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
            Map<String, Constant> taskConstants,
            ValidationContext validationContext) {

        List<TaskMapping> taskMappings = toTaskMappings(processId,
                                                        task,
                                                        taskMappingsMap);
        return taskMappingsValidators
                .stream()
                .flatMap(validator -> validator.validateTaskMappings(taskMappings,
                                                                     taskConstants,
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
