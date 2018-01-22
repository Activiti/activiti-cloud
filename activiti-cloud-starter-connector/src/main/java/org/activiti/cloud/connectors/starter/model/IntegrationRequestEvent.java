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

package org.activiti.cloud.connectors.starter.model;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationRequestEvent {

    private String id;

    private String processInstanceId;

    private String processDefinitionId;

    private String executionId;

    private String flowNodeId;

    private Map<String, Object> variables;

    //used by json deserialization
    public IntegrationRequestEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationRequestEvent(String processInstanceId,
                                   String processDefinitionId,
                                   String executionId,
                                   Map<String, Object> variables) {
        this();
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.executionId = executionId;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public String getFlowNodeId() {
        return flowNodeId;
    }

    @Override
    public String toString() {
        return "IntegrationRequestEvent{" +
                "processInstanceId='" + processInstanceId + '\'' +
                ", processDefinitionId='" + processDefinitionId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", variables=" + variables +
                '}';
    }
}
