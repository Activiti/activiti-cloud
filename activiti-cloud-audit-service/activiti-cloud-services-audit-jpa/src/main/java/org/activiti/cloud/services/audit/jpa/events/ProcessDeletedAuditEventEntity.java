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
package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.activiti.cloud.api.process.model.events.CloudProcessDeletedEvent;
import org.hibernate.annotations.DynamicInsert;

@Entity(name = ProcessDeletedAuditEventEntity.PROCESS_DELETED_EVENT)
@DiscriminatorValue(value = ProcessDeletedAuditEventEntity.PROCESS_DELETED_EVENT)
@DynamicInsert
public class ProcessDeletedAuditEventEntity extends ProcessAuditEventEntity {

    protected static final String PROCESS_DELETED_EVENT = "ProcessDeletedEvent";

    public ProcessDeletedAuditEventEntity() {
    }

    public ProcessDeletedAuditEventEntity(CloudProcessDeletedEvent cloudEvent) {
        super(cloudEvent);
    }

    public ProcessDeletedAuditEventEntity(ProcessAuditEventEntity auditEvent, CloudProcessDeletedEvent cloudEvent) {
        this(cloudEvent);
        setProcessInstance(auditEvent.getProcessInstance());
        setProcessInstanceId(auditEvent.getProcessInstanceId());
        setParentProcessInstanceId(auditEvent.getParentProcessInstanceId());
        setProcessDefinitionId(auditEvent.getProcessDefinitionId());
        setProcessDefinitionKey(auditEvent.getProcessDefinitionKey());
        setEntityId(auditEvent.getEntityId());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProcessDeletedAuditEventEntity [toString()=").append(super.toString()).append("]");
        return builder.toString();
    }
}
