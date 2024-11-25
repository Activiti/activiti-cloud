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
package org.activiti.cloud.api.process.model.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.Set;
import org.activiti.cloud.api.process.model.QueryCloudProcessInstance;
import org.activiti.cloud.api.process.model.QueryCloudSubprocessInstance;

public class QueryCloudSubprocessInstanceImpl implements QueryCloudSubprocessInstance {

    private String id;
    private String processDefinitionName;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Override
    @JsonIgnore
    public Set<? extends QueryCloudProcessInstance> getSubprocesses() {
        return Set.of();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return "";
    }

    @Override
    @JsonIgnore
    public Date getStartDate() {
        return null;
    }

    @Override
    @JsonIgnore
    public Date getCompletedDate() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getInitiator() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getBusinessKey() {
        return "";
    }

    @Override
    @JsonIgnore
    public ProcessInstanceStatus getStatus() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getProcessDefinitionId() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getProcessDefinitionKey() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getParentId() {
        return "";
    }

    @Override
    @JsonIgnore
    public Integer getProcessDefinitionVersion() {
        return 0;
    }

    @Override
    @JsonIgnore
    public String getAppName() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getServiceName() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getServiceFullName() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getServiceType() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getServiceVersion() {
        return "";
    }

    @Override
    @JsonIgnore
    public String getAppVersion() {
        return "";
    }
}
