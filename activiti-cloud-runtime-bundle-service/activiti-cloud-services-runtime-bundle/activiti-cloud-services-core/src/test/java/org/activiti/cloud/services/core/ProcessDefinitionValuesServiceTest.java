/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.engine.RepositoryService;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.ConstantDefinition;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.Mapping;
import org.activiti.spring.process.model.ProcessConstantsMapping;
import org.activiti.spring.process.model.ProcessVariablesMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionValuesServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ProcessExtensionService processExtensionService;

    @InjectMocks
    private ProcessDefinitionValuesService processDefinitionValuesService;

    @Test
    void should_returnStaticValues_when_getProcessModelStaticValuesMappingForStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();
        var extension = new Extension();
        var processVariablesMapping = new ProcessVariablesMapping();
        var mapping = new Mapping();
        mapping.setValue("inputValue");
        mapping.setType(Mapping.SourceMappingType.VALUE);

        startEvent.setId("startEventId");
        startEvent.setFormKey("formKey");
        process.addFlowElement(startEvent);
        process.setInitialFlowElement(startEvent);
        process.setId("processId");
        bpmnModel.addProcess(process);
        processVariablesMapping.getInputs().put("inputKey", mapping);
        extension.getMappings().put("startEventId", processVariablesMapping);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelStaticValuesMappingForStartEvent("processId");

        assertThat(result).hasSize(1).containsEntry("inputKey", "inputValue");
    }

    @Test
    void should_returnEmptyMap_when_noMappingsInGetProcessModelStaticValuesMappingForStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();
        var extension = new Extension();

        startEvent.setId("startEventId");
        startEvent.setFormKey("formKey");
        process.addFlowElement(startEvent);
        process.setInitialFlowElement(startEvent);
        process.setId("processId");
        bpmnModel.addProcess(process);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelStaticValuesMappingForStartEvent("processId");

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmptyStaticValues_when_noFormKeyInStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();

        startEvent.setId("startEventId");
        process.addFlowElement(startEvent);
        process.setInitialFlowElement(startEvent);
        process.setId("processId");
        bpmnModel.addProcess(process);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);

        var result = processDefinitionValuesService.getProcessModelStaticValuesMappingForStartEvent("processId");

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnConstantValues_when_getProcessModelConstantValuesForStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();
        var extension = new Extension();
        var processConstantsMapping = new ProcessConstantsMapping();
        var constantDefinition = new ConstantDefinition();
        constantDefinition.setValue("constantValue");

        process.setId("processId");
        startEvent.setId("startEventId");
        process.addFlowElement(startEvent);
        bpmnModel.addProcess(process);
        processConstantsMapping.put("constantKey", constantDefinition);
        extension.getConstants().put("startEventId", processConstantsMapping);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId");

        assertThat(result).hasSize(1).containsEntry("constantKey", "constantValue");
    }

    @Test
    void should_returnEmptyMap_when_noConstantsInGetProcessModelConstantValuesForStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();
        var extension = new Extension();

        process.setId("processId");
        startEvent.setId("startEventId");
        process.addFlowElement(startEvent);
        bpmnModel.addProcess(process);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId");

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmptyConstantValues_when_noFormKeyInStartEvent() {
        var bpmnModel = new BpmnModel();
        var process = new Process();
        var startEvent = new StartEvent();

        process.setId("processId");
        startEvent.setId("startEventId");
        process.addFlowElement(startEvent);
        bpmnModel.addProcess(process);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);

        var result = processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId");

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnStaticValues_multipleProcesses_when_getProcessModelStaticValuesMappingForStartEvent() {
        var bpmnModel = new BpmnModel();
        var firstProcess = new Process();
        firstProcess.setId("firstProcessId");
        bpmnModel.addProcess(firstProcess);

        var process = new Process();
        var startEvent = new StartEvent();
        var extension = new Extension();
        var processVariablesMapping = new ProcessVariablesMapping();
        var mapping = new Mapping();
        mapping.setValue("inputValue");
        mapping.setType(Mapping.SourceMappingType.VALUE);

        startEvent.setId("startEventId");
        startEvent.setFormKey("formKey");
        process.addFlowElement(startEvent);
        process.setInitialFlowElement(startEvent);
        process.setId("processId");
        bpmnModel.addProcess(process);
        processVariablesMapping.getInputs().put("inputKey", mapping);
        extension.getMappings().put("startEventId", processVariablesMapping);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelStaticValuesMappingForStartEvent("processId");

        assertThat(result).hasSize(1).containsEntry("inputKey", "inputValue");
    }

    @Test
    void should_returnConstantValues_multipleProcesses_when_getProcessModelConstantValuesForStartEvent() {
        var bpmnModel = new BpmnModel();
        var firstProcess = new Process();
        firstProcess.setId("firstProcessId");
        bpmnModel.addProcess(firstProcess);

        var process = new Process();
        process.setId("processId");
        var startEvent = new StartEvent();
        var extension = new Extension();
        var processConstantsMapping = new ProcessConstantsMapping();
        var constantDefinition = new ConstantDefinition();
        constantDefinition.setValue("constantValue");

        startEvent.setId("startEventId");
        process.addFlowElement(startEvent);
        bpmnModel.addProcess(process);
        processConstantsMapping.put("constantKey", constantDefinition);
        extension.getConstants().put("startEventId", processConstantsMapping);

        when(repositoryService.getBpmnModel("processId")).thenReturn(bpmnModel);
        when(processExtensionService.getExtensionsForId("processId")).thenReturn(extension);

        var result = processDefinitionValuesService.getProcessModelConstantValuesForStartEvent("processId");

        assertThat(result).hasSize(1).containsEntry("constantKey", "constantValue");
    }
}
