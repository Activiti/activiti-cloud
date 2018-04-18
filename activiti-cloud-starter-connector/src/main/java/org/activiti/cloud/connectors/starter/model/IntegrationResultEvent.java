/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationResultEvent {

    private String id;

    private String executionId;

    private String flowNodeId;

    private String targetApplication;

    private Map<String, Object> variables;

    private Service service;

    private Application application;

    //used by json deserialization
    public IntegrationResultEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationResultEvent(String executionId,
                                  Map<String, Object> variables,
                                  Service service,
                                  Application application) {
        this();
        this.executionId = executionId;
        this.variables = variables;
    }

    public String getId() {
        return id;
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

    public String getFlowNodeId() {
        return flowNodeId;
    }

    public void setFlowNodeId(String flowNodeId) {
        this.flowNodeId = flowNodeId;
    }

    public String getTargetApplication() {
        return targetApplication;
    }

    public void setTargetApplication(String targetApplication) {
        this.targetApplication = targetApplication;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    @Override
    public String toString() {
        return "IntegrationResultEvent{" +
                "id='" + id + '\'' +
                ", service="+service+
                ", application="+application+
                ", executionId='" + executionId + '\'' +
                ", variables=" + variables +
                '}';
    }
}
