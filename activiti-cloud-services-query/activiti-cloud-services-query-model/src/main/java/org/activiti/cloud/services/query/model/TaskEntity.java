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

package org.activiti.cloud.services.query.model;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "Task")
@Table(name = "TASK",
        indexes = {
                @Index(name = "task_status_idx", columnList = "status", unique = false),
                @Index(name = "task_processInstance_idx", columnList = "processInstanceId", unique = false)
        })
public class TaskEntity extends ActivitiEntityMetadata implements CloudTask {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String assignee;
    private String name;
    private String description;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date dueDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date claimedDate;
    private int priority;
    private String processDefinitionId;
    private String processInstanceId;
    private Integer processDefinitionVersion;
    private String businessKey;
    private String taskDefinitionKey;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    private String owner;
    private String parentTaskId;
    private String formKey;
    private Date completedDate;
    private Long duration;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModified;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date createdFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModifiedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModifiedFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastClaimedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastClaimedFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date completedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date completedFrom;

    @JsonIgnore
    @ManyToOne(optional = true, fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private ProcessInstanceEntity processInstance;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<TaskCandidateUser> taskCandidateUsers;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<TaskCandidateGroup> taskCandidateGroups;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))

    private Set<TaskVariableEntity> variables;

    public TaskEntity() {
    }

    public TaskEntity(CloudTaskCreatedEvent taskCreatedEvent) {
        super(taskCreatedEvent.getServiceName(),
              taskCreatedEvent.getServiceFullName(),
              taskCreatedEvent.getServiceVersion(),
              taskCreatedEvent.getAppName(),
              taskCreatedEvent.getAppVersion());
        Task task = taskCreatedEvent.getEntity();
        this.id = task.getId();
        this.assignee = task.getAssignee();
        this.name = task.getName();
        this.description = task.getDescription();
        this.createdDate = task.getCreatedDate();
        this.dueDate = task.getDueDate();
        this.priority = task.getPriority();
        this.processDefinitionId = task.getProcessDefinitionId();
        this.processInstanceId = task.getProcessInstanceId();
        this.status = task.getStatus();
        this.lastModified = task.getCreatedDate();
        this.claimedDate = task.getClaimedDate();
        this.owner = task.getOwner();
        this.parentTaskId = task.getParentTaskId();
        this.formKey = task.getFormKey();
        this.processDefinitionVersion = taskCreatedEvent.getProcessDefinitionVersion();
        this.businessKey = taskCreatedEvent.getBusinessKey();
        this.taskDefinitionKey = task.getTaskDefinitionKey();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    @Override
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }
    
    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    @Override
    public boolean isStandalone() {
        return getProcessInstanceId() == null;
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }
    
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Transient
    public Date getLastModifiedTo() {
        return lastModifiedTo;
    }

    public void setLastModifiedTo(Date lastModifiedTo) {
        this.lastModifiedTo = lastModifiedTo;
    }

    @Transient
    public Date getLastModifiedFrom() {
        return lastModifiedFrom;
    }

    @Override
    public Date getClaimedDate() {
        return claimedDate;
    }

    public void setClaimedDate(Date claimedDate) {
        this.claimedDate = claimedDate;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setLastModifiedFrom(Date lastModifiedFrom) {
        this.lastModifiedFrom = lastModifiedFrom;
    }

    /**
     * @return the processInstance
     */
    public ProcessInstance getProcessInstance() {
        return this.processInstance;
    }

    /**
     * @param processInstance the processInstance to set
     */
    public void setProcessInstance(ProcessInstanceEntity processInstance) {
        this.processInstance = processInstance;
    }

    /**
     * @return the variableEntities
     */
    public Set<TaskVariableEntity> getVariables() {
        return this.variables;
    }

    /**
     * @param variables the variableEntities to set
     */
    public void setVariables(Set<TaskVariableEntity> variables) {
        this.variables = variables;
    }

    /**
     * @return the taskCandidateUsers
     */
    public Set<TaskCandidateUser> getTaskCandidateUsers() {
        return this.taskCandidateUsers;
    }

    /**
     * @param taskCandidateUsers the taskCandidateUsers to set
     */
    public void setTaskCandidateUsers(Set<TaskCandidateUser> taskCandidateUsers) {
        this.taskCandidateUsers = taskCandidateUsers;
    }

    public List<String> getCandidateUsers(){
        return this.taskCandidateUsers != null ? this.taskCandidateUsers
                       .stream()
                       .map(TaskCandidateUser::getUserId)
                       .collect(Collectors.toList()) : Collections.emptyList();
    }

    public List<String> getCandidateGroups(){
        return this.taskCandidateGroups != null ? this.taskCandidateGroups
                       .stream()
                       .map(TaskCandidateGroup::getGroupId)
                       .collect(Collectors.toList()): Collections.emptyList();
    }

    /**
     * @return the taskCandidateUsers
     */
    public Set<TaskCandidateGroup> getTaskCandidateGroups() {
        return this.taskCandidateGroups;
    }

    /**
     * @param taskCandidateGroups the taskCandidateGroups to set
     */
    public void setTaskCandidateGroups(Set<TaskCandidateGroup> taskCandidateGroups) {
        this.taskCandidateGroups = taskCandidateGroups;
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
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }
    
    @Override
    public Long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    @Transient
    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date endDate) {
        this.completedDate = endDate;
    }
    
    @Transient
    public Date getCreatedTo() {
        return createdTo;
    }
   
    public void setCreatedTo(Date createdTo) {
        this.createdTo = createdTo;
    }

    @Transient
    public Date getCreatedFrom() {
        return createdFrom;
    }
   
    public void setCreatedFrom(Date createdFrom) {
        this.createdFrom = createdFrom;
    }

    @Transient
    public Date getLastClaimedTo() {
        return lastClaimedTo;
    }

    
    public void setLastClaimedTo(Date lastClaimedTo) {
        this.lastClaimedTo = lastClaimedTo;
    }

    @Transient
    public Date getLastClaimedFrom() {
        return lastClaimedFrom;
    }

    
    public void setLastClaimedFrom(Date lastClaimedFrom) {
        this.lastClaimedFrom = lastClaimedFrom;
    }

    @Transient
    public Date getCompletedTo() {
        return completedTo;
    }
   
    public void setCompletedTo(Date completedTo) {
        this.completedTo = completedTo;
    }

    @Transient
    public Date getCompletedFrom() {
        return completedFrom;
    }

    public void setCompletedFrom(Date completedFrom) {
        this.completedFrom = completedFrom;
    }
    
    public boolean isInFinalState(){
        return  !(TaskStatus.CREATED.equals(status) || 
                  TaskStatus.ASSIGNED.equals(status)|| 
                  TaskStatus.SUSPENDED.equals(status));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(id);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskEntity other = (TaskEntity) obj;
        return Objects.equals(id, other.id);
    }
    
}