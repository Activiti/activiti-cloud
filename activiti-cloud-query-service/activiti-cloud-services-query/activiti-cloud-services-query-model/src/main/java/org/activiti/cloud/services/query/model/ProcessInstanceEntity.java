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

import static jakarta.persistence.TemporalType.TIMESTAMP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.annotations.PropertyType;
import com.querydsl.core.annotations.QueryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "ProcessInstance")
@Table(
    name = "PROCESS_INSTANCE",
    indexes = {
        @Index(name = "pi_status_idx", columnList = "status", unique = false),
        @Index(name = "pi_businessKey_idx", columnList = "businessKey", unique = false),
        @Index(name = "pi_name_idx", columnList = "name", unique = false),
        @Index(name = "pi_processDefinitionId_idx", columnList = "processDefinitionId", unique = false),
        @Index(name = "pi_processDefinitionKey_idx", columnList = "processDefinitionKey", unique = false),
        @Index(name = "pi_processDefinitionName_idx", columnList = "processDefinitionName", unique = false),
    }
)
@DynamicInsert
@DynamicUpdate
@NamedEntityGraphs(
    value = {
        @NamedEntityGraph(
            name = "ProcessInstances.withVariables",
            attributeNodes = { @NamedAttributeNode(value = "variables", subgraph = "variables") },
            subgraphs = { @NamedSubgraph(name = "variables", attributeNodes = { @NamedAttributeNode("value") }) }
        ),
    }
)
public class ProcessInstanceEntity extends ActivitiEntityMetadata implements CloudProcessInstance {

    @Id
    private String id;

    private String name;
    private String processDefinitionId;

    @Schema(
        description = "It identifies uniquely the process. In the BPMN process definition file it is the id attribute of a process and in the Modeling application it is usually called as Process ID."
    )
    private String processDefinitionKey;

    private String initiator;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date startDate;

    @Schema(
        description = "The business key associated to the process instance. It could be useful to add a reference to external systems.",
        readOnly = true
    )
    private String businessKey;

    @Enumerated(EnumType.STRING)
    private ProcessInstanceStatus status;

    private Integer processDefinitionVersion;
    private String processDefinitionName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date completedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date suspendedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date lastModified;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date lastModifiedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Temporal(TIMESTAMP)
    private Date lastModifiedFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Temporal(TIMESTAMP)
    private Date startFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Temporal(TIMESTAMP)
    private Date startTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Transient
    private Date completedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Transient
    private Date completedFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Transient
    private Date suspendedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @QueryType(PropertyType.DATETIME)
    @Transient
    private Date suspendedFrom;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private Set<TaskEntity> tasks = new LinkedHashSet<>();

    @JsonView(JsonViews.ProcessVariables.class)
    //@Filter(name = "variablesFilter")
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private Set<ProcessVariableEntity> variables = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private Set<BPMNActivityEntity> activities = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private List<ServiceTaskEntity> serviceTasks = new LinkedList<>();

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "processInstanceId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
        foreignKey = @jakarta.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none")
    )
    private List<BPMNSequenceFlowEntity> sequenceFlows = new LinkedList<>();

    private String parentId;

    public ProcessInstanceEntity() {}

    public ProcessInstanceEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        String processInstanceId,
        String processDefinitionId,
        ProcessInstanceStatus status,
        Date lastModified
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
        this.id = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.status = status;
        this.lastModified = lastModified;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public ProcessInstanceStatus getStatus() {
        return status;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public void setStatus(ProcessInstanceStatus status) {
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

    public void setLastModifiedFrom(Date lastModifiedFrom) {
        this.lastModifiedFrom = lastModifiedFrom;
    }

    public Set<TaskEntity> getTasks() {
        return this.tasks;
    }

    public void setTasks(Set<TaskEntity> tasks) {
        this.tasks = tasks;
    }

    public Set<ProcessVariableEntity> getVariables() {
        return variables;
    }

    public void setVariables(Set<ProcessVariableEntity> variable) {
        this.variables = variable;
    }

    public Optional<ProcessVariableEntity> getVariable(String variableName) {
        return getVariables().stream().filter(v -> v.getName().equals(variableName)).findFirst();
    }

    public Optional<BPMNSequenceFlowEntity> getSequenceFlowByEventId(String eventId) {
        return getSequenceFlows().stream().filter(v -> eventId.equals(v.getEventId())).findFirst();
    }

    @Override
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Transient
    public Date getStartFrom() {
        return startFrom;
    }

    public void setStartFrom(Date startFrom) {
        this.startFrom = startFrom;
    }

    @Transient
    public Date getStartTo() {
        return startTo;
    }

    public void setStartTo(Date startTo) {
        this.startTo = startTo;
    }

    @Override
    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date endDate) {
        this.completedDate = endDate;
    }

    public Date getCompletedTo() {
        return completedTo;
    }

    public void setCompletedTo(Date completedTo) {
        this.completedTo = completedTo;
    }

    public Date getCompletedFrom() {
        return completedFrom;
    }

    public void setCompletedFrom(Date completedFrom) {
        this.completedFrom = completedFrom;
    }

    public Date getSuspendedDate() {
        return suspendedDate;
    }

    public void setSuspendedDate(Date suspendedDate) {
        this.suspendedDate = suspendedDate;
    }

    public Date getSuspendedTo() {
        return suspendedTo;
    }

    public void setSuspendedTo(Date suspendedTo) {
        this.suspendedTo = suspendedTo;
    }

    public Date getSuspendedFrom() {
        return suspendedFrom;
    }

    public void setSuspendedFrom(Date suspendedFrom) {
        this.suspendedFrom = suspendedFrom;
    }

    public boolean isInFinalState() {
        return !(
            ProcessInstanceStatus.CREATED.equals(status) ||
            ProcessInstanceStatus.RUNNING.equals(status) ||
            ProcessInstanceStatus.SUSPENDED.equals(status)
        );
    }

    public Set<BPMNActivityEntity> getActivities() {
        return activities;
    }

    public void setActivities(Set<BPMNActivityEntity> bpmnActivities) {
        this.activities = bpmnActivities;
    }

    public List<ServiceTaskEntity> getServiceTasks() {
        return serviceTasks;
    }

    public void setServiceTasks(List<ServiceTaskEntity> serviceTasks) {
        this.serviceTasks = serviceTasks;
    }

    public List<BPMNSequenceFlowEntity> getSequenceFlows() {
        return sequenceFlows;
    }

    public void setSequenceFlows(List<BPMNSequenceFlowEntity> sequenceFlows) {
        this.sequenceFlows = sequenceFlows;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        ProcessInstanceEntity other = (ProcessInstanceEntity) obj;

        return id != null && Objects.equals(id, other.id);
    }
}
