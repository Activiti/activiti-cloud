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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import org.activiti.cloud.services.audit.jpa.converters.json.ProcessInstanceJpaJsonConverter;
import org.activiti.api.process.model.ProcessInstance;

@Entity
public abstract class ProcessAuditEventEntity extends AuditEventEntity {

    @Convert(converter = ProcessInstanceJpaJsonConverter.class)
    @Lob
    @Column
    private ProcessInstance processInstance;

    private String nestedProcessDefinitionId;
    private String nestedProcessInstanceId;

    public ProcessAuditEventEntity() {
    }

    public ProcessAuditEventEntity(String eventId,
                                   Long timestamp,
                                   String eventType) {
        super(eventId,
              timestamp,
              eventType);
    }

    public ProcessAuditEventEntity(String eventId,
                                   Long timestamp,
                                   String eventType,
                                   String appName,
                                   String appVersion,
                                   String serviceName,
                                   String serviceFullName,
                                   String serviceType,
                                   String serviceVersion,
                                   ProcessInstance processInstance,
                                   String nestedProcessDefinitionId,
                                   String nestedProcessInstanceId) {
        super(eventId,
              timestamp,
              eventType);

        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setProcessDefinitionId((processInstance != null) ? processInstance.getProcessDefinitionId() : null);
        setProcessInstanceId((processInstance != null) ? processInstance.getId() : null);

        this.processInstance = processInstance;
        this.nestedProcessDefinitionId = nestedProcessDefinitionId;
        this.nestedProcessInstanceId = nestedProcessInstanceId;
        setEntityId(processInstance.getId());
    }

    public ProcessAuditEventEntity(String eventId,
                                   Long timestamp,
                                   String eventType,
                                   String appName,
                                   String appVersion,
                                   String serviceName,
                                   String serviceFullName,
                                   String serviceType,
                                   String serviceVersion,
                                   ProcessInstance processInstance) {
        super(eventId,
              timestamp,
              eventType);
        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setProcessDefinitionId((processInstance != null) ? processInstance.getProcessDefinitionId() : null);
        setProcessInstanceId((processInstance != null) ? processInstance.getId() : null);

        this.processInstance = processInstance;
        setEntityId(processInstance.getId());
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public String getNestedProcessDefinitionId() {
        return nestedProcessDefinitionId;
    }

    public String getNestedProcessInstanceId() {
        return nestedProcessInstanceId;
    }

    public void setNestedProcessDefinitionId(String nestedProcessDefinitionId) {
        this.nestedProcessDefinitionId = nestedProcessDefinitionId;
    }

    public void setNestedProcessInstanceId(String nestedProcessInstanceId) {
        this.nestedProcessInstanceId = nestedProcessInstanceId;
    }
}
