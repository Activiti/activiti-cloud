package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessResumedAuditEventEntity;

public class ProcessResumedEventConverter extends BaseEventToEntityConverter {

    public ProcessResumedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name();
    }

    @Override
    protected ProcessResumedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ProcessResumedAuditEventEntity((CloudProcessResumedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessResumedAuditEventEntity processResumedAuditEventEntity = (ProcessResumedAuditEventEntity) auditEventEntity;

        return new CloudProcessResumedEventImpl(processResumedAuditEventEntity.getEventId(),
                                                processResumedAuditEventEntity.getTimestamp(),
                                                processResumedAuditEventEntity.getProcessInstance());
    }
}
