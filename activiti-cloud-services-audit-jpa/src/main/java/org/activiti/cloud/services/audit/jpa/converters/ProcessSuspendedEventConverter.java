package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessSuspendedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessSuspendedAuditEventEntity;

public class ProcessSuspendedEventConverter extends BaseEventToEntityConverter {

    public ProcessSuspendedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name();
    }

    @Override
    protected ProcessSuspendedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {      
        return new ProcessSuspendedAuditEventEntity((CloudProcessSuspendedEvent) cloudRuntimeEvent); 
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessSuspendedAuditEventEntity processSuspendedAuditEventEntity = (ProcessSuspendedAuditEventEntity) auditEventEntity;

        return new CloudProcessSuspendedEventImpl(processSuspendedAuditEventEntity.getEventId(),
                                                  processSuspendedAuditEventEntity.getTimestamp(),
                                                  processSuspendedAuditEventEntity.getProcessInstance());
    }
}
