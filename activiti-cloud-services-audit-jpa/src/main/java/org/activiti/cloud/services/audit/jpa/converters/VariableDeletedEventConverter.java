package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableDeletedEventEntity;

public class VariableDeletedEventConverter extends BaseEventToEntityConverter {

    public VariableDeletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return VariableEvent.VariableEvents.VARIABLE_DELETED.name();
    }

    @Override
    protected VariableDeletedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new VariableDeletedEventEntity((CloudVariableDeletedEvent) cloudRuntimeEvent);       
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        VariableDeletedEventEntity variableDeletedEventEntity = (VariableDeletedEventEntity) auditEventEntity;

        return new CloudVariableDeletedEventImpl(variableDeletedEventEntity.getEventId(),
                                                 variableDeletedEventEntity.getTimestamp(),
                                                 variableDeletedEventEntity.getVariableInstance());
    }
}
