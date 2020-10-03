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

import java.util.Date;
import java.util.Objects;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.springframework.format.annotation.DateTimeFormat;

@MappedSuperclass
public abstract class BaseBPMNActivityEntity extends ActivitiEntityMetadata {

    /** The unique identifier of this historic activity instance. */
    @Id
    private String id;

    /** The unique identifier of the activity in the process */
    private String elementId;

    /** The display name for the activity */
    private String activityName;

    /** The XML tag of the activity as in the process file */
    private String activityType;

    /** The associated process instance id */
    private String processInstanceId;

    /** The associated process definition id */
    private String processDefinitionId;

    /** The associated execution id */
    private String executionId;

    /** The current state of activity */
    @Enumerated(EnumType.STRING)
    private BPMNActivityStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date startedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date completedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date cancelledDate;

    /** The associated process definition key of the activity as in the process file */
    private String processDefinitionKey;

    /** The associated deployed process definition version of the activity */
    private Integer processDefinitionVersion;

    /** The associated business key of the activity as in the process instance */
    private String businessKey;

    public BaseBPMNActivityEntity() { }

    public BaseBPMNActivityEntity(String serviceName,
                              String serviceFullName,
                              String serviceVersion,
                              String appName,
                              String appVersion) {
        super(serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion);
    }

    public String getId() {
        return id;
    }

    public String getElementId() {
        return elementId;
    };

    public String getActivityName() {
        return activityName;
    };

    public String getActivityType() {
        return activityType;
    };

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public BPMNActivityStatus getStatus() {
        return status;
    }

    public void setStatus(BPMNActivityStatus status) {
        this.status = status;
    }

    public Date getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public Date getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(Date cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(activityName,
                                               activityType,
                                               businessKey,
                                               cancelledDate,
                                               completedDate,
                                               elementId,
                                               id,
                                               processDefinitionId,
                                               processDefinitionKey,
                                               processDefinitionVersion,
                                               processInstanceId,
                                               executionId,
                                               startedDate,
                                               status);
        return result;
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
        BaseBPMNActivityEntity other = (BaseBPMNActivityEntity) obj;
        return Objects.equals(activityName, other.activityName) &&
               Objects.equals(activityType, other.activityType) &&
               Objects.equals(businessKey, other.businessKey) &&
               Objects.equals(cancelledDate, other.cancelledDate) &&
               Objects.equals(completedDate, other.completedDate) &&
               Objects.equals(elementId, other.elementId) &&
               Objects.equals(id, other.id) &&
               Objects.equals(processDefinitionId, other.processDefinitionId) &&
               Objects.equals(processDefinitionKey, other.processDefinitionKey) &&
               Objects.equals(processDefinitionVersion, other.processDefinitionVersion) &&
               Objects.equals(processInstanceId, other.processInstanceId) &&
               Objects.equals(executionId, other.executionId) &&
               Objects.equals(startedDate, other.startedDate) &&
               status == other.status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BPMNActivityEntity [id=")
               .append(id)
               .append(", elementId=")
               .append(elementId)
               .append(", activityName=")
               .append(activityName)
               .append(", activityType=")
               .append(activityType)
               .append(", processInstanceId=")
               .append(processInstanceId)
               .append(", executionId=")
               .append(executionId)
               .append(", processDefinitionId=")
               .append(processDefinitionId)
               .append(", status=")
               .append(status)
               .append(", startedDate=")
               .append(startedDate)
               .append(", completedDate=")
               .append(completedDate)
               .append(", cancelledDate=")
               .append(cancelledDate)
               .append(", processDefinitionKey=")
               .append(processDefinitionKey)
               .append(", processDefinitionVersion=")
               .append(processDefinitionVersion)
               .append(", businessKey=")
               .append(businessKey)
               .append(", toString()=")
               .append(super.toString())
               .append("]");

        return builder.toString();
    }

}