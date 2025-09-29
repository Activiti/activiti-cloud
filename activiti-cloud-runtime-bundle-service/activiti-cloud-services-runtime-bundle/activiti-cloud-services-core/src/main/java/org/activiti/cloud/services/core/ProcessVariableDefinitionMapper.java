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

import org.activiti.spring.process.model.ProcessVariableDefinition;
import org.activiti.spring.process.model.VariableDefinition;

public class ProcessVariableDefinitionMapper {

    public ProcessVariableDefinition mapToProcessVariableDefinition(
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
