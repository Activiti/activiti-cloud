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
package org.activiti.cloud.api.task.model.impl;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.task.model.CloudTask;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CloudTaskImpl extends CloudRuntimeEntityImpl implements CloudTask {

    private String id;
    private String owner;
    private String assignee;
    private String name;
    private String description;
    private Date createdDate;
    private Date claimedDate;
    private Date dueDate;
    private int priority;
    private String processDefinitionId;
    private String processInstanceId;
    private String parentTaskId;
    private String formKey;
    private TaskStatus status;
    private Date completedDate;
    private Long duration;
    private Integer processDefinitionVersion;
    private String businessKey;
    private String taskDefinitionKey;
    private List<String> candidateUsers;
    private List<String> candidateGroups;
    private String processDefinitionName;

    public CloudTaskImpl() {
    }

    public CloudTaskImpl(Task task) {
        super(task);
        id = task.getId();
        owner = task.getOwner();
        assignee = task.getAssignee();
        name = task.getName();
        description = task.getDescription();
        createdDate = task.getCreatedDate();
        claimedDate = task.getClaimedDate();
        dueDate = task.getDueDate();
        priority = task.getPriority();
        processDefinitionId = task.getProcessDefinitionId();
        processInstanceId = task.getProcessInstanceId();
        parentTaskId = task.getParentTaskId();
        formKey = task.getFormKey();
        status = task.getStatus();
        processDefinitionVersion = task.getProcessDefinitionVersion();
        businessKey = task.getBusinessKey();
        taskDefinitionKey = task.getTaskDefinitionKey();
        candidateUsers = task.getCandidateUsers();
        candidateGroups = task.getCandidateGroups();
        processDefinitionName = task.getProcessDefinitionName();
    }

    @Override
    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    @Override
    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Date getClaimedDate() {
        return claimedDate;
    }

    public void setClaimedDate(Date claimedDate) {
        this.claimedDate = claimedDate;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    @Override
    public boolean isStandalone() {
        return this.getProcessInstanceId() == null;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    @Override
    public Long getDuration() {
        return duration;
    }

    @Override
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Override
    public String toString() {
        return "CloudTaskImpl{" +
               "id='" + id + '\'' +
               ", owner='" + owner + '\'' +
               ", assignee='" + assignee + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", createdDate=" + createdDate +
               ", claimedDate=" + claimedDate +
               ", dueDate=" + dueDate +
               ", priority=" + priority +
               ", processDefinitionId='" + processDefinitionId + '\'' +
               ", processInstanceId='" + processInstanceId + '\'' +
               ", parentTaskId='" + parentTaskId + '\'' +
               ", formKey='" + formKey + '\'' +
               ", status=" + status +
               ", processDefinitionVersion=" + processDefinitionVersion +
               ", businessKey=" + businessKey +
               ", taskDefinitionKey=" + taskDefinitionKey +
               ", processDefinitionName=" + processDefinitionName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudTaskImpl task = (CloudTaskImpl) o;
        return priority == task.priority &&
               Objects.equals(id,
                              task.id) &&
               Objects.equals(owner,
                              task.owner) &&
               Objects.equals(assignee,
                              task.assignee) &&
               Objects.equals(name,
                              task.name) &&
               Objects.equals(description,
                              task.description) &&
               Objects.equals(createdDate,
                              task.createdDate) &&
               Objects.equals(claimedDate,
                              task.claimedDate) &&
               Objects.equals(dueDate,
                              task.dueDate) &&
               Objects.equals(processDefinitionId,
                              task.processDefinitionId) &&
               Objects.equals(processInstanceId,
                              task.processInstanceId) &&
               Objects.equals(parentTaskId,
                              task.parentTaskId) &&
               Objects.equals(formKey,
                              task.formKey) &&
               Objects.equals(processDefinitionVersion,
                              task.processDefinitionVersion) &&
               Objects.equals(businessKey,
                              task.businessKey) && 
               Objects.equals(processDefinitionName,
                        task.processDefinitionName) &&
               status == task.status;
    }
}

