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
package org.activiti.cloud.api.model.shared.impl.events;

import java.util.Objects;
import org.activiti.api.runtime.event.impl.RuntimeEventImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;

public abstract class CloudRuntimeEventImpl<ENTITY_TYPE, EVENT_TYPE extends Enum<?>>
    extends RuntimeEventImpl<ENTITY_TYPE, EVENT_TYPE>
    implements CloudRuntimeEvent<ENTITY_TYPE, EVENT_TYPE> {

    private String appName;
    private String serviceFullName;
    private String appVersion;
    private String serviceName;
    private String serviceVersion;
    private String serviceType;
    private String entityId;
    private Integer sequenceNumber;
    private String messageId;
    private String actor = "service_user";

    public CloudRuntimeEventImpl() {}

    public CloudRuntimeEventImpl(ENTITY_TYPE entity) {
        super(entity);
    }

    public CloudRuntimeEventImpl(String id, Long timestamp, ENTITY_TYPE entity) {
        super(id, timestamp, entity);
    }

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    @Override
    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getActor() {
        return this.actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudRuntimeEventImpl [appName=")
            .append(appName)
            .append(", serviceFullName=")
            .append(serviceFullName)
            .append(", appVersion=")
            .append(appVersion)
            .append(", serviceName=")
            .append(serviceName)
            .append(", serviceVersion=")
            .append(serviceVersion)
            .append(", serviceType=")
            .append(serviceType)
            .append(", entityId=")
            .append(entityId)
            .append(", sequenceNumber=")
            .append(sequenceNumber)
            .append(", messageId=")
            .append(messageId)
            .append(", actor=")
            .append(actor)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            appName,
            appVersion,
            entityId,
            messageId,
            actor,
            sequenceNumber,
            serviceFullName,
            serviceName,
            serviceType,
            serviceVersion,
            super.hashCode()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudRuntimeEventImpl other = (CloudRuntimeEventImpl) obj;
        return (
            Objects.equals(appName, other.appName) &&
            Objects.equals(appVersion, other.appVersion) &&
            Objects.equals(entityId, other.entityId) &&
            Objects.equals(messageId, other.messageId) &&
            Objects.equals(actor, other.actor) &&
            Objects.equals(sequenceNumber, other.sequenceNumber) &&
            Objects.equals(serviceFullName, other.serviceFullName) &&
            Objects.equals(serviceName, other.serviceName) &&
            Objects.equals(serviceType, other.serviceType) &&
            Objects.equals(serviceVersion, other.serviceVersion)
        );
    }
}
