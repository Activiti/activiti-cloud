/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.cloud.services.core.decorator.ProcessDefinitionDecorator;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.ProcessVariableDefinition;
import org.activiti.spring.process.model.VariableDefinition;

public class ProcessDefinitionService extends BaseProcessDefinitionService {

    private final ProcessRuntime processRuntime;

    private final ProcessExtensionService processExtensionService;

    public ProcessDefinitionService(
        ProcessRuntime processRuntime,
        List<ProcessDefinitionDecorator> processDefinitionDecorators,
        ProcessExtensionService processExtensionService
    ) {
        super(processDefinitionDecorators);
        this.processRuntime = processRuntime;
        this.processExtensionService = processExtensionService;
    }

    public Page<ProcessDefinition> getProcessDefinitions(
        Pageable pageable,
        List<String> include,
        String excludedCategory
    ) {
        GetProcessDefinitionsPayload processDefinitionsPayload = buildGetProcessDefinitionsPayload(excludedCategory);
        Page<ProcessDefinition> processDefinitions = processRuntime.processDefinitions(
            pageable,
            processDefinitionsPayload
        );
        processDefinitions.getContent().replaceAll(processDefinition -> super.decorateAll(processDefinition, include));
        return processDefinitions;
    }

    public Set<ProcessVariableDefinition> getProcessVariableDefinitions() {
        Set<ProcessVariableDefinition> customProcessVariables = new HashSet<>();

        processRuntime
            .processDefinitions()
            .forEach(processDefinition ->
                processExtensionService
                    .getExtensionsForWithoutCallingDB(processDefinition)
                    .getProperties()
                    .values()
                    .stream()
                    .filter(VariableDefinition::getDisplay)
                    .forEach(variableDefinition ->
                        customProcessVariables.add(
                            mapToProcessVariableDefinition(variableDefinition, processDefinition)
                        )
                    )
            );

        return customProcessVariables;
    }

    private ProcessVariableDefinition mapToProcessVariableDefinition(
        VariableDefinition variableDefinition,
        org.activiti.engine.repository.ProcessDefinition processDefinition
    ) {
        ProcessVariableDefinition processVariableDefinition = new ProcessVariableDefinition();
        processVariableDefinition.setName(variableDefinition.getName());
        processVariableDefinition.setType(variableDefinition.getType());
        processVariableDefinition.setProcessDefinitionId(processDefinition.getId());
        processVariableDefinition.setProcessDefinitionKey(processDefinition.getKey());
        processVariableDefinition.setProcessDefinitionName(processDefinition.getName());
        processVariableDefinition.setId(variableDefinition.getId());
        processVariableDefinition.setDescription(variableDefinition.getDescription());
        processVariableDefinition.setRequired(variableDefinition.isRequired());
        processVariableDefinition.setDisplay(variableDefinition.getDisplay());
        processVariableDefinition.setDisplayName(variableDefinition.getDisplayName());
        processVariableDefinition.setAnalytics(variableDefinition.isAnalytics());
        processVariableDefinition.setEphemeral(variableDefinition.isEphemeral());
        processVariableDefinition.setValue(variableDefinition.getValue());
        return processVariableDefinition;
    }
}
