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

import java.util.Date;
import java.util.Set;

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
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name="Task")
@Table(name = "TASK",
	indexes= {
		@Index(name="task_status_idx", columnList="status", unique=false),
		@Index(name="task_processInstance_idx", columnList="processInstanceId", unique=false)
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
    private String category;
    private String processDefinitionId;
    private String processInstanceId;
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    private String owner;
    private String parentTaskId;
    private String formKey;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date lastModified;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date lastModifiedTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date lastModifiedFrom;

    @JsonIgnore
    @ManyToOne(optional = true, fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private ProcessInstanceEntity processInstance;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<TaskCandidateUser> taskCandidateUsers;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<TaskCandidateGroup> taskCandidateGroups;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskId", referencedColumnName = "id", insertable = false, updatable = false
            , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))

    private Set<TaskVariableEntity> variables;

    public TaskEntity() {
    }

    public TaskEntity(String id,
                      String assignee,
                      String name,
                      String description,
                      Date createTime,
                      Date dueDate,
                      int priority,
                      String category,
                      String processDefinitionId,
                      String processInstanceId,
                      String serviceName,
                      String serviceFullName,
                      String serviceVersion,
                      String appName,
                      String appVersion,
                      TaskStatus status,
                      Date lastModified,
                      Date claimedDate,
                      String owner,
                      String parentTaskId,
                      String formKey) {
        super(serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion);
        this.id = id;
        this.assignee = assignee;
        this.name = name;
        this.description = description;
        this.createdDate = createTime;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.lastModified = lastModified;
        this.claimedDate = claimedDate;
        this.owner = owner;
        this.parentTaskId = parentTaskId;
        this.formKey = formKey;
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

    public String getCategory() {
        return category;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public boolean isStandAlone() {
        return processInstanceId == null;
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
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
}
