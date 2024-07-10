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
package org.activiti.cloud.services.query.rest.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskDto {

    private final String id;
    private final String owner;
    private final String assignee;
    private final String name;
    private final String description;
    private final Date createdDate;
    private final Date claimedDate;
    private final Date dueDate;
    private final int priority;
    private final String processDefinitionId;
    private final String processInstanceId;
    private final String parentTaskId;
    private final String formKey;
    private final String completedBy;
    private final String status;
    private final Date completedDate;
    private final Long duration;
    private final Integer processDefinitionVersion;
    private final String businessKey;
    private final String taskDefinitionKey;
    private final List<String> candidateUsers;
    private final List<String> candidateGroups;
    private final String processDefinitionName;
    private final List<String> permissions;
    private final Date lastModified;
    private Map<String, Object> processVariables;

    public TaskDto(TaskEntity entity) {
        this.id = entity.getId();
        this.owner = entity.getOwner();
        this.assignee = entity.getAssignee();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.createdDate = entity.getCreatedDate();
        this.claimedDate = entity.getClaimedDate();
        this.dueDate = entity.getDueDate();
        this.priority = entity.getPriority();
        this.processDefinitionId = entity.getProcessDefinitionId();
        this.processInstanceId = entity.getProcessInstanceId();
        this.parentTaskId = entity.getParentTaskId();
        this.formKey = entity.getFormKey();
        this.completedBy = entity.getCompletedBy();
        this.status = entity.getStatus() == null ? null : entity.getStatus().name();
        this.completedDate = entity.getCompletedDate();
        this.duration = entity.getDuration();
        this.processDefinitionVersion = entity.getProcessDefinitionVersion();
        this.businessKey = entity.getBusinessKey();
        this.taskDefinitionKey = entity.getTaskDefinitionKey();
        this.candidateUsers = entity.getCandidateUsers();
        this.candidateGroups = entity.getCandidateGroups();
        this.processDefinitionName = entity.getProcessDefinitionName();
        this.permissions =
            entity.getPermissions() == null ? null : entity.getPermissions().stream().map(Enum::name).toList();
        this.lastModified = entity.getLastModified();
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getClaimedDate() {
        return claimedDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public String getFormKey() {
        return formKey;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public String getStatus() {
        return status;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public Long getDuration() {
        return duration;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setProcessVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public Date getLastModified() {
        return lastModified;
    }
}
