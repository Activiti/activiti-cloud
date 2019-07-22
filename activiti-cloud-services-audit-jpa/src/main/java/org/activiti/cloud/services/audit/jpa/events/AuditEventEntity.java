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

package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE")
@Entity
public abstract class AuditEventEntity {

    @Id
    @GeneratedValue
    private Long id;
    protected String eventId;
    protected Long timestamp;
    protected String eventType;

    /* Cloud Data */
    private String appName;
    private String appVersion;
    private String serviceName;
    private String serviceFullName;
    private String serviceType;
    private String serviceVersion;
    private int sequenceNumber;
    private String messageId;

    /* base Process Data */
    private String entityId;
    private String processDefinitionId;
    private String processInstanceId;
    private String processDefinitionKey;
    private String parentProcessInstanceId;
    private String businessKey;
    
    public AuditEventEntity() {
    }

    public AuditEventEntity(String eventId,
                            Long timestamp,
                            String eventType) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    public AuditEventEntity(CloudRuntimeEvent<?,?> cloudEvent) {
        this.eventId = cloudEvent.getId();
        this.timestamp = cloudEvent.getTimestamp();
        this.eventType = cloudEvent.getEventType().name();
        this.appName = cloudEvent.getAppName();
        this.appVersion = cloudEvent.getAppVersion();
        this.serviceName = cloudEvent.getServiceName();
        this.serviceFullName = cloudEvent.getServiceFullName();
        this.serviceType = cloudEvent.getServiceType();
        this.serviceVersion = cloudEvent.getServiceVersion();
        this.messageId = cloudEvent.getMessageId();
        this.sequenceNumber = cloudEvent.getSequenceNumber();
        this.entityId = cloudEvent.getEntityId();
        this.processInstanceId = cloudEvent.getProcessInstanceId();
        this.processDefinitionId = cloudEvent.getProcessDefinitionId();
        this.processDefinitionKey = cloudEvent.getProcessDefinitionKey();
        this.businessKey = cloudEvent.getBusinessKey();
        this.parentProcessInstanceId = cloudEvent.getParentProcessInstanceId();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequeceNumber) {
        this.sequenceNumber = sequeceNumber;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
