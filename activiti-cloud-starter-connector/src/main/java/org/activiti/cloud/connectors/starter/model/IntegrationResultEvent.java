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

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationResultEvent {

    private String id;

    private String executionId;

    private String flowNodeId;

    private String targetApplication;

    private Map<String, Object> variables;

    private String serviceName;

    private String serviceFullName;

    private String serviceType;

    private String serviceVersion;

    private String appName;

    private String appVersion;

    //used by json deserialization
    public IntegrationResultEvent() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationResultEvent(String executionId,
                                  Map<String, Object> variables,
                                  String serviceName,
                                  String serviceFullName,
                                  String serviceType,
                                  String serviceVersion,
                                  String appName,
                                  String appVersion) {
        this();
        this.executionId = executionId;
        this.variables = variables;
        this.serviceName = serviceName;
        this.serviceFullName = serviceFullName;
        this.serviceType = serviceType;
        this.serviceVersion = serviceVersion;
        this.appName = appName;
        this.appVersion = appVersion;
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

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String toString() {
        return "IntegrationResultEvent{" +
                "id='" + id + '\'' +
                ", serviceName="+serviceName+ '\'' +
                ", serviceFullName="+serviceFullName+ '\'' +
                ", serviceType="+serviceType+ '\'' +
                ", serviceVersion="+serviceVersion+ '\'' +
                ", appName="+appName+ '\'' +
                ", appVersion="+appVersion+ '\'' +
                ", executionId='" + executionId + '\'' +
                ", variables=" + variables +
                '}';
    }
}
