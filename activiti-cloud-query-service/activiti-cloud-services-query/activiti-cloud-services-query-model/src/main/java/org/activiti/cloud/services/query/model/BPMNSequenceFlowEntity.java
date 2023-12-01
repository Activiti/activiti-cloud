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

import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;
import org.activiti.api.process.model.BPMNSequenceFlow;
import org.hibernate.annotations.Immutable;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "BPMNSequenceFlow")
@Table(
    name = "BPMN_SEQUENCE_FLOW",
    indexes = {
        @Index(name = "bpmn_sequence_flow_processInstance_idx", columnList = "processInstanceId", unique = false),
        @Index(name = "bpmn_sequence_flow_elementId_idx", columnList = "elementId", unique = false),
        @Index(
            name = "bpmn_sequence_flow_processInstance_elementId_idx",
            columnList = "processInstanceId,elementId",
            unique = false
        ),
        @Index(name = "bpmn_sequence_flow_eventId_idx", columnList = "eventId", unique = true),
    }
)
@Immutable
public class BPMNSequenceFlowEntity extends ActivitiEntityMetadata implements BPMNSequenceFlow {

    /** The unique identifier of this historic activity instance. */
    @Id
    private String id;

    /** The associated process instance id */
    private String processInstanceId;

    /** The associated process definition id */
    private String processDefinitionId;

    /** The date/time of the sequence flow was taken */
    @Column(name = "taken_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date date;

    /** The XML tag of the source activity as in the process file */
    private String sourceActivityElementId;

    /** The display name for the source activity */
    private String sourceActivityName;

    /** The type for the source activity */
    private String sourceActivityType;

    /** The XML tag of the target activity as in the process file */
    private String targetActivityElementId;

    /** The display name for the target activity */
    private String targetActivityName;

    /** The type for the target activity */
    private String targetActivityType;

    /** The XML tag of the activity as in the process file */
    private String elementId;

    /** The associated process definition key of the activity as in the process file */
    private String processDefinitionKey;

    /** The associated deployed process definition version of the activity */
    private Integer processDefinitionVersion;

    /** The associated business key of the activity as in the process instance */
    private String businessKey;

    /** The associated eventId of event */
    private String eventId;

    public BPMNSequenceFlowEntity() {}

    public BPMNSequenceFlowEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getSourceActivityElementId() {
        return sourceActivityElementId;
    }

    @Override
    public String getSourceActivityName() {
        return sourceActivityName;
    }

    @Override
    public String getSourceActivityType() {
        return sourceActivityType;
    }

    @Override
    public String getTargetActivityElementId() {
        return targetActivityElementId;
    }

    @Override
    public String getTargetActivityName() {
        return targetActivityName;
    }

    @Override
    public String getTargetActivityType() {
        return targetActivityType;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public void setSourceActivityElementId(String sourceActivityElementId) {
        this.sourceActivityElementId = sourceActivityElementId;
    }

    public void setSourceActivityName(String sourceActivityName) {
        this.sourceActivityName = sourceActivityName;
    }

    public void setTargetActivityElementId(String targetActivityElementId) {
        this.targetActivityElementId = targetActivityElementId;
    }

    public void setSourceActivityType(String sourceActivityType) {
        this.sourceActivityType = sourceActivityType;
    }

    public void setTargetActivityName(String targetActivityName) {
        this.targetActivityName = targetActivityName;
    }

    public void setTargetActivityType(String targetActivityType) {
        this.targetActivityType = targetActivityType;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BPMNSequenceFlowEntity other = (BPMNSequenceFlowEntity) obj;

        return id != null && Objects.equals(this.getId(), other.getId());
    }
}
