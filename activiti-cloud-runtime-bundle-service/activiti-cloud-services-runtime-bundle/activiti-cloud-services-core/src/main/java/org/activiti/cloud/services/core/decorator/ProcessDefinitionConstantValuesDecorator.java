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
package org.activiti.cloud.services.core.decorator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.api.process.model.ExtendedCloudProcessDefinition;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessConstantsMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDefinitionConstantValuesDecorator implements ProcessDefinitionDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDefinitionConstantValuesDecorator.class);

    private static final String HANDLED_VALUE = "constant-values";

    private final ProcessExtensionService processExtensionService;
    private final RepositoryService repositoryService;

    public ProcessDefinitionConstantValuesDecorator(
        ProcessExtensionService processExtensionService,
        RepositoryService repositoryService
    ) {
        this.processExtensionService = processExtensionService;
        this.repositoryService = repositoryService;
    }

    @Override
    public String getHandledValue() {
        return HANDLED_VALUE;
    }

    @Override
    public ExtendedCloudProcessDefinition decorate(ExtendedCloudProcessDefinition processDefinition) {
        var constantValues = getConstantValuesForStartEvent(processDefinition);
        processDefinition.getConstantValues().putAll(constantValues);
        return processDefinition;
    }

    public Map<String, Object> getConstantValuesForStartEvent(ExtendedCloudProcessDefinition processDefinition) {
        try {
            FlowElement startEvent = getProcessStartEvent(processDefinition);
            Extension extension = processExtensionService.getExtensionsForId(processDefinition.getId());

            return getConstantValues(extension, startEvent);
        } catch (Exception e) {
            LOGGER.error("Error getting process model constant values mapping for start event", e);
        }
        return Collections.emptyMap();
    }

    private FlowElement getProcessStartEvent(ExtendedCloudProcessDefinition processDefinition) {
        Process process = getProcessById(processDefinition);
        return process.getInitialFlowElement();
    }

    private Process getProcessById(ExtendedCloudProcessDefinition cloudProcessDefinition) {
        String processDefinitionId = cloudProcessDefinition.getId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        return bpmnModel.getProcessById(cloudProcessDefinition.getKey());
    }

    private static Map<String, Object> getConstantValues(Extension extension, FlowElement startEvent) {
        Map<String, Object> constantValues = new HashMap<>();

        ProcessConstantsMapping startEventConstants = extension.getConstantForFlowElement(startEvent.getId());

        if (startEventConstants != null) {
            startEventConstants.forEach((input, mapping) -> constantValues.put(input, mapping.getValue()));
        }
        return constantValues;
    }
}
