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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent;
import org.hibernate.annotations.DynamicInsert;

@Entity(name = MessageSentAuditEventEntity.MESSAGE_SENT_EVENT)
@DiscriminatorValue(value = MessageSentAuditEventEntity.MESSAGE_SENT_EVENT)
@DynamicInsert
public class MessageSentAuditEventEntity extends MessageAuditEventEntity {

    protected static final String MESSAGE_SENT_EVENT = "MessageSentEvent";

    public MessageSentAuditEventEntity() {}

    public MessageSentAuditEventEntity(CloudBPMNMessageSentEvent cloudEvent) {
        super(cloudEvent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MessageSentAuditEventEntity []");
        return builder.toString();
    }
}
