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
package org.activiti.cloud.services.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.Mapping.SourceMappingType;
import org.activiti.spring.process.model.ProcessConstantsMapping;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.apache.commons.lang3.StringUtils;

public class ProcessDefinitionValuesService {

    private final RepositoryService repositoryService;

    private final ProcessExtensionService processExtensionService;

    public ProcessDefinitionValuesService(
        RepositoryService repositoryService,
        ProcessExtensionService processExtensionService
    ) {
        this.repositoryService = repositoryService;
        this.processExtensionService = processExtensionService;
    }

    public Map<String, Object> getProcessModelStaticValuesMappingForStartEvent(String id) {
        Map<String, Object> result = new HashMap<>();
        ExtensionsStartEventId extensionsStartEventId = getProcessExtensionsForStartEvent(id, true);
        if (
            extensionsStartEventId != null &&
            extensionsStartEventId.extensions() != null &&
            extensionsStartEventId.id() != null
        ) {
            ProcessVariablesMapping startEventMappings = extensionsStartEventId
                .extensions()
                .getMappings()
                .get(extensionsStartEventId.id());

            if (startEventMappings != null) {
                startEventMappings
                    .getInputs()
                    .forEach((input, mapping) -> {
                        if (SourceMappingType.VALUE.equals(mapping.getType())) {
                            result.put(input, mapping.getValue());
                        }
                    });
            }
        }

        return result;
    }

    public Map<String, Object> getProcessModelConstantValuesForStartEvent(String id) {
        Map<String, Object> result = new HashMap<>();
        ExtensionsStartEventId extensionsStartEventId = getProcessExtensionsForStartEvent(id, false);
        if (
            extensionsStartEventId != null &&
            extensionsStartEventId.extensions() != null &&
            extensionsStartEventId.id() != null
        ) {
            ProcessConstantsMapping startEventConstants = extensionsStartEventId
                .extensions()
                .getConstantForFlowElement(extensionsStartEventId.id());

            if (startEventConstants != null) {
                startEventConstants.keySet().forEach(key -> result.put(key, startEventConstants.get(key).getValue()));
            }
        }

        return result;
    }

    private ExtensionsStartEventId getProcessExtensionsForStartEvent(String id, boolean formRequired) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(id);
        Process process = bpmnModel.getProcessById(getProcessId(id));

        if (!formRequired || bpmnModel.getStartFormKey(process.getId()) != null) {
            Optional<FlowElement> startEvent = process
                .getFlowElements()
                .stream()
                .filter(flowElement -> flowElement.getClass().equals(StartEvent.class))
                .findFirst();

            if (startEvent.isPresent()) {
                return new ExtensionsStartEventId(
                    startEvent.get().getId(),
                    processExtensionService.getExtensionsForId(id)
                );
            }
        }

        return null;
    }

    private record ExtensionsStartEventId(String id, Extension extensions) {}

    private String getProcessId(String processDefinitionId) {
        return Optional
            .ofNullable(processDefinitionId)
            .filter(StringUtils::isNotBlank)
            .map(id -> id.split(":")[0])
            .orElse(null);
    }
}
