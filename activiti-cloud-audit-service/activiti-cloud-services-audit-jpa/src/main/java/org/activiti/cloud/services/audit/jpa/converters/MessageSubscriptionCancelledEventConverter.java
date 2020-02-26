package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudMessageSubscriptionCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.MessageSubscriptionCancelledAuditEventEntity;

public class MessageSubscriptionCancelledEventConverter extends BaseEventToEntityConverter {

    public MessageSubscriptionCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED.name();
    }

    @Override
    protected MessageSubscriptionCancelledAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new MessageSubscriptionCancelledAuditEventEntity((CloudMessageSubscriptionCancelledEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        MessageSubscriptionCancelledAuditEventEntity messageSubscriptionCancelledAuditEventEntity = (MessageSubscriptionCancelledAuditEventEntity) auditEventEntity;

        return CloudMessageSubscriptionCancelledEventImpl.builder()
                .withEntity(messageSubscriptionCancelledAuditEventEntity.getMessageSubscription())
                .build();
    }
}
