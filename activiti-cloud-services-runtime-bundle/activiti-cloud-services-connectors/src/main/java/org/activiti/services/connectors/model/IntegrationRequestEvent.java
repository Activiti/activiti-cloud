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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationRequestEvent {

    private String id;

    private String processInstanceId;

    private String processDefinitionId;

    private String executionId;

    private String integrationContextId;

    private String flowNodeId;

    private String connectorType;

    private String applicationName;

    private Map<String, Object> variables;

    //used by json deserialization
    public IntegrationRequestEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationRequestEvent(DelegateExecution execution,
                                   IntegrationContextEntity integrationContext,
                                   String applicationName) {
        this();
        this.processInstanceId = execution.getProcessInstanceId();
        this.processDefinitionId = execution.getProcessDefinitionId();
        this.executionId = integrationContext.getExecutionId();
        this.flowNodeId = integrationContext.getFlowNodeId();
        this.variables = execution.getVariables();
        this.integrationContextId = integrationContext.getId();
        this.applicationName = applicationName;
        this.connectorType = ((ServiceTask) execution.getCurrentFlowElement()).getImplementation();
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

    public String getIntegrationContextId() {
        return integrationContextId;
    }

    public String getFlowNodeId() {
        return flowNodeId;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }
}
