package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableUpdatedEventEntity;

public class VariableUpdatedEventConverter extends BaseEventToEntityConverter {

    public VariableUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED.name();
    }

    @Override
    protected VariableUpdatedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new VariableUpdatedEventEntity((CloudVariableUpdatedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        VariableUpdatedEventEntity variableUpdatedEventEntity = (VariableUpdatedEventEntity) auditEventEntity;

        return new CloudVariableUpdatedEventImpl(variableUpdatedEventEntity.getEventId(),
                                                 variableUpdatedEventEntity.getTimestamp(),
                                                 variableUpdatedEventEntity.getVariableInstance());
    }
}
