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
        this.service = service;
        this.application = application;
    }

    public String getId() {
        return id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getFlowNodeId() {
        return flowNodeId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}
