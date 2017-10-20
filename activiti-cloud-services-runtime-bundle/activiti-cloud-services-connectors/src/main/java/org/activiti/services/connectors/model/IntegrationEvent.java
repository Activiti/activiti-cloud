/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.services.connectors.model;

import java.util.HashMap;
import java.util.Map;

public class IntegrationEvent {

    private String processInstanceId;

    private String processDefinitionId;

    private String executionId;

    private Map<String, Object> variables;

    //used by json deserialization
    public IntegrationEvent() {
    }

    public IntegrationEvent(String processInstanceId,
                            String processDefinitionId,
                            String executionId,
                            Map<String, Object> variables) {
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
        this.variables = variables;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public void putVariable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(name, value);
    }

}
