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


import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageReceivedAuditEventEntity;

public class MessageReceivedEventConverter extends BaseEventToEntityConverter {

    public MessageReceivedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED.name();
    }

    @Override
    protected MessageReceivedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new MessageReceivedAuditEventEntity(CloudBPMNMessageReceivedEvent.class.cast(cloudRuntimeEvent));
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        MessageReceivedAuditEventEntity messageEventEntity = (MessageReceivedAuditEventEntity) auditEventEntity;

        return new CloudBPMNMessageReceivedEventImpl(messageEventEntity.getEventId(),
                                                     messageEventEntity.getTimestamp(),
                                                     messageEventEntity.getMessage(),
                                                     messageEventEntity.getProcessDefinitionId(),
                                                     messageEventEntity.getProcessInstanceId());
    }
}
