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
package org.activiti.cloud.services.audit.jpa.controllers.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

public class CsvLogEntry implements CloudRuntimeEvent {

    @CsvBindByName
    private String time;

    @CsvBindByName
    private Integer sequenceNumber;

    @CsvBindByName
    private String messageId;

    @CsvBindByName
    private String entityId;

    @CsvBindByName
    private String id;

    @CsvCustomBindByName(converter = ObjectToJsonConvertor.class)
    private Object entity;

    @CsvBindByName
    private Enum<?> eventType;

    @CsvBindByName
    private String appVersion;

    @CsvBindByName
    private String serviceVersion;

    @CsvBindByName
    private String serviceType;

    @CsvBindByName
    private String serviceFullName;

    @CsvBindByName
    private String processInstanceId;

    @CsvBindByName
    private String appName;

    @CsvBindByName
    private String serviceName;

    @CsvBindByName
    private String businessKey;

    @CsvBindByName
    private String parentProcessInstanceId;

    @CsvBindByName
    private String processDefinitionId;

    @CsvBindByName
    private String processDefinitionKey;

    @CsvBindByName
    private Integer processDefinitionVersion;

    @CsvBindByName
    private String actor;

    public CsvLogEntry(CloudRuntimeEvent event) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.time = dateFormat.format(new Date(event.getTimestamp()));
        this.sequenceNumber = event.getSequenceNumber();
        this.messageId = event.getMessageId();
        this.entityId = event.getEntityId();
        this.id = event.getId();
        this.entity = event.getEntity();
        this.eventType = event.getEventType();
        this.appVersion = event.getAppVersion();
        this.serviceVersion = event.getServiceVersion();
        this.serviceType = event.getServiceType();
        this.serviceFullName = event.getServiceFullName();
        this.processInstanceId = event.getProcessInstanceId();
        this.appName = event.getAppName();
        this.serviceName = event.getServiceName();
        this.businessKey = event.getBusinessKey();
        this.parentProcessInstanceId = event.getParentProcessInstanceId();
        this.processDefinitionId = event.getProcessDefinitionId();
        this.processDefinitionKey = event.getProcessDefinitionKey();
        this.processDefinitionVersion = event.getProcessDefinitionVersion();
        this.actor = event.getActor();
    }

    public String getTime() {
        return this.time;
    }

    @Override
    public Integer getSequenceNumber() {
        return this.sequenceNumber;
    }

    @Override
    public String getMessageId() {
        return this.messageId;
    }

    @Override
    public String getEntityId() {
        return this.entityId;
    }

    @Override
    public String getActor() {
        return this.actor;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Object getEntity() {
        return this.entity;
    }

    @Override
    public Long getTimestamp() {
        // ignoring, replaced by time property that returns human-readable date
        return null;
    }

    @Override
    public Enum<?> getEventType() {
        return this.eventType;
    }

    @Override
    public String getProcessInstanceId() {
        return this.processInstanceId;
    }

    @Override
    public String getParentProcessInstanceId() {
        return this.parentProcessInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return this.processDefinitionId;
    }

    @Override
    public String getProcessDefinitionKey() {
        return this.processDefinitionKey;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return this.processDefinitionVersion;
    }

    @Override
    public String getBusinessKey() {
        return this.businessKey;
    }

    @Override
    public String getAppName() {
        return this.appName;
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String getServiceFullName() {
        return this.serviceFullName;
    }

    @Override
    public String getServiceType() {
        return this.serviceType;
    }

    @Override
    public String getServiceVersion() {
        return this.serviceVersion;
    }

    @Override
    public String getAppVersion() {
        return this.appVersion;
    }
}
