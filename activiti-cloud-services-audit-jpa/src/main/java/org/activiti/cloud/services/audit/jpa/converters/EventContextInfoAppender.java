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
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class EventContextInfoAppender {

    public AuditEventEntity addProcessContextInfoToEntityEvent(AuditEventEntity auditEventEntity,
                                                               CloudRuntimeEvent apiEvent) {
        auditEventEntity.setEntityId(apiEvent.getEntityId());
        auditEventEntity.setProcessInstanceId(apiEvent.getProcessInstanceId());
        auditEventEntity.setProcessDefinitionId(apiEvent.getProcessDefinitionId());
        auditEventEntity.setProcessDefinitionKey(apiEvent.getProcessDefinitionKey());
        auditEventEntity.setBusinessKey(apiEvent.getBusinessKey());
        auditEventEntity.setParentProcessInstanceId(apiEvent.getParentProcessInstanceId());
        return auditEventEntity;
    }

    public CloudRuntimeEvent addProcessContextInfoToApiEvent(CloudRuntimeEventImpl<?,?> apiEvent,
                                                             AuditEventEntity auditEventEntity) {
        apiEvent.setEntityId(auditEventEntity.getEntityId());
        apiEvent.setProcessInstanceId(auditEventEntity.getProcessInstanceId());
        apiEvent.setProcessDefinitionId(auditEventEntity.getProcessDefinitionId());
        apiEvent.setProcessDefinitionKey(auditEventEntity.getProcessDefinitionKey());
        apiEvent.setBusinessKey(auditEventEntity.getBusinessKey());
        apiEvent.setParentProcessInstanceId(auditEventEntity.getParentProcessInstanceId());
        return apiEvent;
    }

}
