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
package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessVariableInstance implements CloudVariableInstance {

    private String processInstanceId;
    private String processDefinitionKey;
    private String name;
    private Serializable value;
    private String type;
    private Date createdTime;
    private Date lastUpdatedTime;
    private String appVersion;
    private String serviceVersion;
    private String serviceType;
    private String serviceFullName;
    private String serviceName;

    private String appName;

    public ProcessVariableInstance() {}

    public ProcessVariableInstance(
        String processInstanceId,
        String type,
        String name,
        String processDefinitionKey,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        Date createdTime,
        Date lastUpdatedTime,
        Serializable value
    ) {
        this.processInstanceId = processInstanceId;
        this.type = type;
        this.name = name;
        this.processDefinitionKey = processDefinitionKey;
        this.serviceName = serviceName;
        this.serviceFullName = serviceFullName;
        this.serviceVersion = serviceVersion;
        this.appName = appName;
        this.appVersion = appVersion;
        this.createdTime = createdTime;
        this.lastUpdatedTime = lastUpdatedTime;
        this.value = value;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public ProcessVariableKey getProcessVariableKey() {
        return new ProcessVariableKey(processDefinitionKey, name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getProcessInstanceId() {
        return this.processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getTaskId() {
        return null;
    }

    @Override
    public boolean isTaskVariable() {
        return false;
    }

    @Override
    public Serializable getValue() {
        return this.value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public Date getLastUpdatedTime() {
        return this.lastUpdatedTime;
    }

    public void setLastUpdatedTime(Date time) {
        this.lastUpdatedTime = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessVariableInstance that = (ProcessVariableInstance) o;
        return (
            Objects.equals(processInstanceId, that.processInstanceId) &&
            Objects.equals(processDefinitionKey, that.processDefinitionKey) &&
            Objects.equals(name, that.name) &&
            Objects.equals(value, that.value)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(processInstanceId, processDefinitionKey, name, value);
    }

    @Override
    public String getAppName() {
        return this.appName;
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String getServiceFullName() {
        return this.serviceFullName;
    }

    @Override
    public String getServiceType() {
        return this.serviceType;
    }

    @Override
    public String getServiceVersion() {
        return this.serviceVersion;
    }

    @Override
    public String getAppVersion() {
        return this.appVersion;
    }
}
