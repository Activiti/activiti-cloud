package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;

public class ProcessStartedEventConverter extends BaseEventToEntityConverter {
   
    public ProcessStartedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name();
    }

    @Override
    protected ProcessStartedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ProcessStartedAuditEventEntity((CloudProcessStartedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessStartedAuditEventEntity processStartedAuditEventEntity = (ProcessStartedAuditEventEntity) auditEventEntity;

        return new CloudProcessStartedEventImpl(processStartedAuditEventEntity.getEventId(),
                                                processStartedAuditEventEntity.getTimestamp(),
                                                processStartedAuditEventEntity.getProcessInstance());
    }
}
