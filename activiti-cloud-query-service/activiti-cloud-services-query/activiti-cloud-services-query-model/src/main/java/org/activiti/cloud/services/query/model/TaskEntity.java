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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "Task")
@Table(
    name = "TASK",
    indexes = {
        @Index(name = "task_status_idx", columnList = "status", unique = false),
        @Index(name = "task_processInstance_idx", columnList = "processInstanceId", unique = false),
        @Index(name = "task_processDefinitionName_idx", columnList = "processDefinitionName", unique = false),
    }
)
@DynamicInsert
@DynamicUpdate
public class TaskEntity extends ActivitiEntityMetadata implements QueryCloudTask, Serializable {

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

    @Column(nullable = true, insertable = true, updatable = false)
    private String processInstanceId;

    private Integer processDefinitionVersion;
    private String processDefinitionName;
    private String businessKey;
    private String taskDefinitionKey;
    private String completedBy;

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
    @Transient
    @QueryType(PropertyType.DATETIME)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date dueDateTo;

    @JsonIgnore
    @Transient
    @QueryType(PropertyType.DATETIME)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date dueDateFrom;

    @JsonIgnore
    @Transient
    @QueryType(PropertyType.STRING)
    private String candidateGroupId;

    @Transient
    private List<TaskPermissions> permissions;

    @JsonIgnore
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private ProcessInstanceEntity processInstance;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "taskId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<TaskCandidateUserEntity> taskCandidateUsers = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "taskId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<TaskCandidateGroupEntity> taskCandidateGroups = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "taskId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private Set<TaskVariableEntity> variables = new LinkedHashSet<>();

    @JsonView(JsonViews.ProcessVariables.class)
    @Filter(name = "variablesFilter")
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(
        name = "task_process_variable",
        joinColumns = { @JoinColumn(name = "task_id") },
        inverseJoinColumns = { @JoinColumn(name = "process_variable_id") }
    )
    private Set<ProcessVariableEntity> processVariables = new LinkedHashSet<>();

    public TaskEntity() {}

    public TaskEntity(CloudTaskCreatedEvent taskCreatedEvent) {
        super(
            taskCreatedEvent.getServiceName(),
            taskCreatedEvent.getServiceFullName(),
            taskCreatedEvent.getServiceVersion(),
            taskCreatedEvent.getAppName(),
            taskCreatedEvent.getAppVersion()
        );
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
    public String getProcessDefinitionName() {
        return processDefinitionName;
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

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
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
    public Set<TaskCandidateUserEntity> getTaskCandidateUsers() {
        return this.taskCandidateUsers;
    }

    /**
     * @param taskCandidateUsers the taskCandidateUsers to set
     */
    public void setTaskCandidateUsers(Set<TaskCandidateUserEntity> taskCandidateUsers) {
        this.taskCandidateUsers = taskCandidateUsers;
    }

    @Override
    public List<String> getCandidateUsers() {
        return this.taskCandidateUsers != null
            ? this.taskCandidateUsers.stream().map(TaskCandidateUserEntity::getUserId).collect(Collectors.toList())
            : Collections.emptyList();
    }

    @Override
    public List<String> getCandidateGroups() {
        return this.taskCandidateGroups != null
            ? this.taskCandidateGroups.stream().map(TaskCandidateGroupEntity::getGroupId).collect(Collectors.toList())
            : Collections.emptyList();
    }

    /**
     * @return the taskCandidateUsers
     */
    public Set<TaskCandidateGroupEntity> getTaskCandidateGroups() {
        return this.taskCandidateGroups;
    }

    /**
     * @param taskCandidateGroups the taskCandidateGroups to set
     */
    public void setTaskCandidateGroups(Set<TaskCandidateGroupEntity> taskCandidateGroups) {
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

    @Override
    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }

    public Optional<TaskVariableEntity> getVariable(String variableName) {
        return getVariables().stream().filter(v -> v.getName().equals(variableName)).findFirst();
    }

    public boolean isInFinalState() {
        return !(
            TaskStatus.CREATED.equals(status) ||
            TaskStatus.ASSIGNED.equals(status) ||
            TaskStatus.SUSPENDED.equals(status)
        );
    }

    @Override
    public List<TaskPermissions> getPermissions() {
        return this.permissions;
    }

    @Override
    public void setPermissions(List<TaskPermissions> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Set<ProcessVariableEntity> getProcessVariables() {
        return processVariables;
    }

    public void setProcessVariables(Set<ProcessVariableEntity> processVariables) {
        this.processVariables = new LinkedHashSet<>();
        this.processVariables.addAll(processVariables);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        TaskEntity other = (TaskEntity) obj;
        return this.id != null && Objects.equals(id, other.id);
    }
}
