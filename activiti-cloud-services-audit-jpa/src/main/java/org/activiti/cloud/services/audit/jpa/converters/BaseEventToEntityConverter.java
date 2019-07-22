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

package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public abstract class BaseEventToEntityConverter implements EventToEntityConverter<AuditEventEntity> {

    private EventContextInfoAppender eventContextInfoAppender;

    protected BaseEventToEntityConverter(EventContextInfoAppender eventContextInfoAppender) {
        this.eventContextInfoAppender = eventContextInfoAppender;
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        AuditEventEntity auditEventEntity = createEventEntity(cloudRuntimeEvent);
        
        return eventContextInfoAppender.addProcessContextInfoToEntityEvent(auditEventEntity, cloudRuntimeEvent);
    }

    protected abstract AuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent);
    

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        CloudRuntimeEventImpl<?, ?> apiEvent = createAPIEvent(auditEventEntity);
        apiEvent.setAppName(auditEventEntity.getAppName());
        apiEvent.setAppVersion(auditEventEntity.getAppVersion());
        apiEvent.setServiceFullName(auditEventEntity.getServiceFullName());
        apiEvent.setServiceName(auditEventEntity.getServiceName());
        apiEvent.setServiceType(auditEventEntity.getServiceType());
        apiEvent.setServiceVersion(auditEventEntity.getServiceVersion());
        apiEvent.setMessageId(auditEventEntity.getMessageId());
        apiEvent.setSequenceNumber(auditEventEntity.getSequenceNumber());
        return eventContextInfoAppender.addProcessContextInfoToApiEvent(apiEvent, auditEventEntity);
    }

    protected abstract CloudRuntimeEventImpl<?,?> createAPIEvent(AuditEventEntity auditEventEntity);

}
