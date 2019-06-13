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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name="ProcessInstance")
@Table(name = "PROCESS_INSTANCE",
		indexes= {
				@Index(name="pi_status_idx", columnList="status", unique=false),
				@Index(name="pi_businessKey_idx", columnList="businessKey", unique=false),
				@Index(name="pi_name_idx", columnList="name", unique=false),
				@Index(name="pi_processDefinitionId_idx", columnList="processDefinitionId", unique=false),
				@Index(name="pi_processDefinitionKey_idx", columnList="processDefinitionKey", unique=false)
		})
public class ProcessInstanceEntity extends ActivitiEntityMetadata implements CloudProcessInstance {

    @Id
    private String id;
    private String name;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String initiator;
    private Date startDate;
    private String businessKey;
    @Enumerated(EnumType.STRING)
    private ProcessInstanceStatus status;
    private Integer processDefinitionVersion;
 
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModified;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModifiedTo;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date lastModifiedFrom;

    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date startFrom;
    
    @JsonIgnore
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date startTo;
    
    @JsonIgnore
    @OneToMany(fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false
    	, foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<TaskEntity> tasks;

    @JsonIgnore
    @OneToMany(fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false
		, foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<ProcessVariableEntity> variables;

    @JsonIgnore
    @OneToMany(fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false
        , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private Set<BPMNActivityEntity> activities;

    @JsonIgnore
    @OneToMany(fetch=FetchType.LAZY)
    @JoinColumn(name = "processInstanceId", referencedColumnName = "id", insertable = false, updatable = false
        , foreignKey = @javax.persistence.ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    private List<BPMNSequenceFlowEntity> sequenceFlows;
    
    private String parentId;

    public ProcessInstanceEntity() {
    }

    public ProcessInstanceEntity(String serviceName,
                                 String serviceFullName,
                                 String serviceVersion,
                                 String appName,
                                 String appVersion,
                                 String processInstanceId,
                                 String processDefinitionId,
                                 ProcessInstanceStatus status,
                                 Date lastModified) {
        super(serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion);
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
    
    public boolean isInFinalState(){
        return  !(ProcessInstanceStatus.CREATED.equals(status) || 
                  ProcessInstanceStatus.RUNNING.equals(status)|| 
                  ProcessInstanceStatus.SUSPENDED.equals(status));
    }

    
    public Set<BPMNActivityEntity> getActivities() {
        return activities;
    }

    
    public void setActivities(Set<BPMNActivityEntity> bpmnActivities) {
        this.activities = bpmnActivities;
    }

    
    public List<BPMNSequenceFlowEntity> getSequenceFlows() {
        return sequenceFlows;
    }

    
    public void setSequenceFlows(List<BPMNSequenceFlowEntity> sequenceFlows) {
        this.sequenceFlows = sequenceFlows;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(businessKey,
                                               id,
                                               initiator,
                                               lastModified,
                                               lastModifiedFrom,
                                               lastModifiedTo,
                                               name,
                                               parentId,
                                               processDefinitionId,
                                               processDefinitionKey,
                                               processDefinitionVersion,
                                               startDate,
                                               startFrom,
                                               startTo,
                                               status);
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
        ProcessInstanceEntity other = (ProcessInstanceEntity) obj;
        return Objects.equals(businessKey, other.businessKey) && 
               Objects.equals(id, other.id) && 
               Objects.equals(initiator, other.initiator) && 
               Objects.equals(lastModified, other.lastModified) && 
               Objects.equals(lastModifiedFrom, other.lastModifiedFrom) && 
               Objects.equals(lastModifiedTo, other.lastModifiedTo) && 
               Objects.equals(name, other.name) && 
               Objects.equals(parentId, other.parentId) && 
               Objects.equals(processDefinitionId, other.processDefinitionId) && 
               Objects.equals(processDefinitionKey, other.processDefinitionKey) && 
               Objects.equals(processDefinitionVersion, other.processDefinitionVersion) && 
               Objects.equals(startDate, other.startDate) && 
               Objects.equals(startFrom, other.startFrom) && 
               Objects.equals(startTo, other.startTo) && 
               status == other.status;
    }
    

    

}