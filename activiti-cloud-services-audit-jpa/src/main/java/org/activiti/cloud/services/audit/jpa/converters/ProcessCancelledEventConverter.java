package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;

public class ProcessCancelledEventConverter extends BaseEventToEntityConverter {

    public ProcessCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name();
    }

    @Override
    protected ProcessCancelledAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ProcessCancelledAuditEventEntity((CloudProcessCancelledEvent) cloudRuntimeEvent); 
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessCancelledAuditEventEntity processCancelledAuditEventEntity = (ProcessCancelledAuditEventEntity) auditEventEntity;
        CloudProcessCancelledEventImpl cloudEvent = new CloudProcessCancelledEventImpl(processCancelledAuditEventEntity.getEventId(),
                                                                                       processCancelledAuditEventEntity.getTimestamp(),
                                                                                       processCancelledAuditEventEntity.getProcessInstance());
        cloudEvent.setCause(processCancelledAuditEventEntity.getCause());
        return cloudEvent;
    }
}
