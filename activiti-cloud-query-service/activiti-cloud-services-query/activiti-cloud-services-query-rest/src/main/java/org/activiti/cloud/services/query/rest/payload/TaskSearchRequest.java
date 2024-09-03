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
package org.activiti.cloud.services.query.rest.payload;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public final class TaskSearchRequest {

    private boolean onlyStandalone;
    private boolean onlyRoot;
    private List<String> name;
    private List<String> description;
    private List<Integer> priority;
    private List<Task.TaskStatus> status;
    private List<String> completedBy;
    private List<String> assignee;
    private Date createdFrom;
    private Date createdTo;
    private Date lastModifiedFrom;
    private Date lastModifiedTo;
    private Date lastClaimedFrom;
    private Date lastClaimedTo;
    private Date dueDateFrom;
    private Date dueDateTo;
    private Date completedFrom;
    private Date completedTo;
    private List<String> candidateUserId;
    private List<String> candidateGroupId;
    private Set<VariableFilter> taskVariableFilters;
    private Set<VariableFilter> processVariableFilters;
    private Set<ProcessVariableKey> processVariableKeys;

    public TaskSearchRequest() {}

    public TaskSearchRequest(
        boolean onlyStandalone,
        boolean onlyRoot,
        List<String> name,
        List<String> description,
        List<Integer> priority,
        List<Task.TaskStatus> status,
        List<String> completedBy,
        List<String> assignee,
        Date createdFrom,
        Date createdTo,
        Date lastModifiedFrom,
        Date lastModifiedTo,
        Date lastClaimedFrom,
        Date lastClaimedTo,
        Date dueDateFrom,
        Date dueDateTo,
        Date completedFrom,
        Date completedTo,
        List<String> candidateUserId,
        List<String> candidateGroupId,
        Set<VariableFilter> taskVariableFilters,
        Set<VariableFilter> processVariableFilters,
        Set<ProcessVariableKey> processVariableKeys
    ) {
        this.onlyStandalone = onlyStandalone;
        this.onlyRoot = onlyRoot;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.completedBy = completedBy;
        this.assignee = assignee;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.lastModifiedFrom = lastModifiedFrom;
        this.lastModifiedTo = lastModifiedTo;
        this.lastClaimedFrom = lastClaimedFrom;
        this.lastClaimedTo = lastClaimedTo;
        this.dueDateFrom = dueDateFrom;
        this.dueDateTo = dueDateTo;
        this.completedFrom = completedFrom;
        this.completedTo = completedTo;
        this.candidateUserId = candidateUserId;
        this.candidateGroupId = candidateGroupId;
        this.taskVariableFilters = taskVariableFilters;
        this.processVariableFilters = processVariableFilters;
        this.processVariableKeys = processVariableKeys;
    }

    public boolean isOnlyStandalone() {
        return onlyStandalone;
    }

    public void setOnlyStandalone(boolean onlyStandalone) {
        this.onlyStandalone = onlyStandalone;
    }

    public boolean isOnlyRoot() {
        return onlyRoot;
    }

    public void setOnlyRoot(boolean onlyRoot) {
        this.onlyRoot = onlyRoot;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<Integer> getPriority() {
        return priority;
    }

    public void setPriority(List<Integer> priority) {
        this.priority = priority;
    }

    public List<Task.TaskStatus> getStatus() {
        return status;
    }

    public void setStatus(List<Task.TaskStatus> status) {
        this.status = status;
    }

    public List<String> getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(List<String> completedBy) {
        this.completedBy = completedBy;
    }

    public List<String> getAssignee() {
        return assignee;
    }

    public void setAssignee(List<String> assignee) {
        this.assignee = assignee;
    }

    public Date getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(Date createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Date getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(Date createdTo) {
        this.createdTo = createdTo;
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

    public Date getLastClaimedFrom() {
        return lastClaimedFrom;
    }

    public void setLastClaimedFrom(Date lastClaimedFrom) {
        this.lastClaimedFrom = lastClaimedFrom;
    }

    public Date getLastClaimedTo() {
        return lastClaimedTo;
    }

    public void setLastClaimedTo(Date lastClaimedTo) {
        this.lastClaimedTo = lastClaimedTo;
    }

    public Date getDueDateFrom() {
        return dueDateFrom;
    }

    public void setDueDateFrom(Date dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }

    public Date getDueDateTo() {
        return dueDateTo;
    }

    public void setDueDateTo(Date dueDateTo) {
        this.dueDateTo = dueDateTo;
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

    public List<String> getCandidateUserId() {
        return candidateUserId;
    }

    public void setCandidateUserId(List<String> candidateUserId) {
        this.candidateUserId = candidateUserId;
    }

    public List<String> getCandidateGroupId() {
        return candidateGroupId;
    }

    public void setCandidateGroupId(List<String> candidateGroupId) {
        this.candidateGroupId = candidateGroupId;
    }

    public Set<VariableFilter> getTaskVariableFilters() {
        return taskVariableFilters;
    }

    public void setTaskVariableFilters(Set<VariableFilter> taskVariableFilters) {
        this.taskVariableFilters = taskVariableFilters;
    }

    public Set<VariableFilter> getProcessVariableFilters() {
        return processVariableFilters;
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
}
