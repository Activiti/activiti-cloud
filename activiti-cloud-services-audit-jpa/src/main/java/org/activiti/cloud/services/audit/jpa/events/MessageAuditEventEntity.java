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

import org.activiti.api.process.model.BPMNMessage;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.MessageJpaJsonConverter;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class MessageAuditEventEntity extends AuditEventEntity {

    @Convert(converter = MessageJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private BPMNMessage message;

    public MessageAuditEventEntity() {
    }

    public MessageAuditEventEntity(CloudBPMNMessageEvent cloudEvent) {
        super(cloudEvent);
        this.message = cloudEvent.getEntity();
        if (message != null) {
            setProcessDefinitionId(message.getProcessDefinitionId());
            setProcessInstanceId(message.getProcessInstanceId());
            setEntityId(message.getElementId());
        }
    }

    public BPMNMessage getMessage() {
        return message;
    }

    public void setMessage(BPMNMessage message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(message);
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
        MessageAuditEventEntity other = (MessageAuditEventEntity) obj;
        return Objects.equals(message, other.message);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MessageAuditEventEntity [message=")
               .append(message)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }
}
