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
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.activiti.api.process.model.MessageSubscription;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.MessageSubscriptionJpaJsonConverter;

@Entity
@DiscriminatorValue(value = MessageSubscriptionCancelledAuditEventEntity.MESSAGE_SUBSCRIPTION_CANCELLED_EVENT)
public class MessageSubscriptionCancelledAuditEventEntity extends AuditEventEntity {

    protected static final String MESSAGE_SUBSCRIPTION_CANCELLED_EVENT = "MsgSubscriptionCancelledEvent";
    
    @Convert(converter = MessageSubscriptionJpaJsonConverter.class)
    @Column(columnDefinition="text")
    private MessageSubscription messageSubscription;

    public MessageSubscriptionCancelledAuditEventEntity() {
    }

    public MessageSubscriptionCancelledAuditEventEntity(CloudMessageSubscriptionCancelledEvent cloudEvent) {
        super(cloudEvent);
        setMessageSubscription(cloudEvent.getEntity()) ;
        if (messageSubscription != null) {
            setProcessDefinitionId(messageSubscription.getProcessDefinitionId());
            setProcessInstanceId(messageSubscription.getProcessInstanceId());
            setEntityId(messageSubscription.getId());
        }
    }
    
    public MessageSubscription getMessageSubscription() {
        return messageSubscription;
    }

    public void setMessageSubscription(MessageSubscription messageSubscription) {
        this.messageSubscription = messageSubscription;
    }
}
