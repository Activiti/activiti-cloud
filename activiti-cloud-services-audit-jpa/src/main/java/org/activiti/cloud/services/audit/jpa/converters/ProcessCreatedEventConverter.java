package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;

public class ProcessCreatedEventConverter  extends BaseEventToEntityConverter {

    public ProcessCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name();
    }

    @Override
    protected ProcessCreatedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ProcessCreatedAuditEventEntity((CloudProcessCreatedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessCreatedAuditEventEntity processCreatedAuditEventEntity = (ProcessCreatedAuditEventEntity) auditEventEntity;

        return new CloudProcessCreatedEventImpl(processCreatedAuditEventEntity.getEventId(),
                                                processCreatedAuditEventEntity.getTimestamp(),
                                                processCreatedAuditEventEntity.getProcessInstance());
    }
}
