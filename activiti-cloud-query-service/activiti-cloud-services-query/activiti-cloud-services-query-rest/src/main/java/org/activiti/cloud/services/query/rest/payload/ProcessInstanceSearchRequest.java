/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.rest.payload;

import java.util.Date;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public class ProcessInstanceSearchRequest implements CloudRuntimeEntityFilterRequest {

    private Set<String> id;
    private Set<String> parentId;
    private Set<String> name;
    private Set<String> processDefinitionName;
    private Set<String> initiator;
    private Set<String> appVersion;
    private Set<ProcessInstance.ProcessInstanceStatus> status;
    private Date lastModifiedFrom;
    private Date lastModifiedTo;
    private Date startFrom;
    private Date startTo;
    private Date completedFrom;
    private Date completedTo;
    private Date suspendedFrom;
    private Date suspendedTo;
    private Set<VariableFilter> processVariableFilters;
    private Set<ProcessVariableKey> processVariableKeys;
    private CloudRuntimeEntitySort sort;
    private Boolean includeSubprocesses;
    private Set<String> linkedProcessInstanceId;
    private Set<String> linkedProcessInstanceType;

    public ProcessInstanceSearchRequest() {}

    public ProcessInstanceSearchRequest(
        Set<String> id,
        Set<String> parentId,
        Set<String> name,
        Set<String> processDefinitionName,
        Set<String> initiator,
        Set<String> appVersion,
        Set<ProcessInstance.ProcessInstanceStatus> status,
        Date lastModifiedFrom,
        Date lastModifiedTo,
        Date startFrom,
        Date startTo,
        Date completedFrom,
        Date completedTo,
        Date suspendedFrom,
        Date suspendedTo,
        Set<VariableFilter> processVariableFilters,
        Set<ProcessVariableKey> processVariableKeys,
        CloudRuntimeEntitySort sort,
        Boolean includeSubprocesses,
        Set<String> linkedProcessInstanceId,
        Set<String> linkedProcessInstanceType
    ) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.processDefinitionName = processDefinitionName;
        this.initiator = initiator;
        this.appVersion = appVersion;
        this.status = status;
        this.lastModifiedFrom = lastModifiedFrom;
        this.lastModifiedTo = lastModifiedTo;
        this.startFrom = startFrom;
        this.startTo = startTo;
        this.completedFrom = completedFrom;
        this.completedTo = completedTo;
        this.suspendedFrom = suspendedFrom;
        this.suspendedTo = suspendedTo;
        this.processVariableFilters = processVariableFilters;
        this.processVariableKeys = processVariableKeys;
        this.sort = sort;
        this.includeSubprocesses = includeSubprocesses;
        this.linkedProcessInstanceId = linkedProcessInstanceId;
        this.linkedProcessInstanceType = linkedProcessInstanceType;
    }

    @Override
    public Set<String> id() {
        return getId();
    }

    public Set<String> getId() {
        return id;
    }

    public Set<String> getParentId() {
        return parentId;
    }

    public Set<VariableFilter> getProcessVariableFilters() {
        return processVariableFilters;
    }

    @Override
    public Set<String> parentId() {
        return getParentId();
    }

    @Override
    public Set<VariableFilter> processVariableFilters() {
        return getProcessVariableFilters();
    }

    @Override
    public CloudRuntimeEntitySort sort() {
        return sort;
    }

    public void setId(Set<String> id) {
        this.id = id;
    }

    public void setParentId(Set<String> parentId) {
        this.parentId = parentId;
    }

    public Set<String> getName() {
        return name;
    }

    public void setName(Set<String> name) {
        this.name = name;
    }

    public Set<String> getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(Set<String> processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public Set<String> getInitiator() {
        return initiator;
    }

    public void setInitiator(Set<String> initiator) {
        this.initiator = initiator;
    }

    public Set<String> getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(Set<String> appVersion) {
        this.appVersion = appVersion;
    }

    public Set<ProcessInstance.ProcessInstanceStatus> getStatus() {
        return status;
    }

    public void setStatus(Set<ProcessInstance.ProcessInstanceStatus> status) {
        this.status = status;
    }

    public Date getLastModifiedFrom() {
        return lastModifiedFrom;
    }

    public void setLastModifiedFrom(Date lastModifiedFrom) {
        this.lastModifiedFrom = lastModifiedFrom;
    }

    public Date getLastModifiedTo() {
        return lastModifiedTo;
    }

    public void setLastModifiedTo(Date lastModifiedTo) {
        this.lastModifiedTo = lastModifiedTo;
    }

    public Date getStartFrom() {
        return startFrom;
    }

    public void setStartFrom(Date startFrom) {
        this.startFrom = startFrom;
    }

    public Date getStartTo() {
        return startTo;
    }

    public void setStartTo(Date startTo) {
        this.startTo = startTo;
    }

    public Date getCompletedFrom() {
        return completedFrom;
    }

    public void setCompletedFrom(Date completedFrom) {
        this.completedFrom = completedFrom;
    }

    public Date getCompletedTo() {
        return completedTo;
    }

    public void setCompletedTo(Date completedTo) {
        this.completedTo = completedTo;
    }

    public Date getSuspendedFrom() {
        return suspendedFrom;
    }

    public void setSuspendedFrom(Date suspendedFrom) {
        this.suspendedFrom = suspendedFrom;
    }

    public Date getSuspendedTo() {
        return suspendedTo;
    }

    public void setSuspendedTo(Date suspendedTo) {
        this.suspendedTo = suspendedTo;
    }

    public void setProcessVariableFilters(Set<VariableFilter> processVariableFilters) {
        this.processVariableFilters = processVariableFilters;
    }

    public Set<ProcessVariableKey> getProcessVariableKeys() {
        return processVariableKeys;
    }

    public void setProcessVariableKeys(Set<ProcessVariableKey> processVariableKeys) {
        this.processVariableKeys = processVariableKeys;
    }

    public CloudRuntimeEntitySort getSort() {
        return sort;
    }

    public void setSort(CloudRuntimeEntitySort sort) {
        this.sort = sort;
    }

    public Boolean getIncludeSubprocesses() {
        return includeSubprocesses != null ? includeSubprocesses : true;
    }

    public void setIncludeSubprocesses(Boolean includeSubprocesses) {
        this.includeSubprocesses = includeSubprocesses;
    }

    public Set<String> getLinkedProcessInstanceId() {
        return linkedProcessInstanceId;
    }

    public void setLinkedProcessInstanceId(Set<String> linkedProcessInstanceId) {
        this.linkedProcessInstanceId = linkedProcessInstanceId;
    }

    public Set<String> getLinkedProcessInstanceType() {
        return linkedProcessInstanceType;
    }

    public void setLinkedProcessInstanceType(Set<String> linkedProcessInstanceType) {
        this.linkedProcessInstanceType = linkedProcessInstanceType;
    }
}
