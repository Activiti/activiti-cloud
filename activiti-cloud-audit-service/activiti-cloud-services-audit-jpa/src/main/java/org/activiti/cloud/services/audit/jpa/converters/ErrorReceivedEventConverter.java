package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNErrorReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ErrorReceivedAuditEventEntity;

public class ErrorReceivedEventConverter extends BaseEventToEntityConverter {

    public ErrorReceivedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED.name();
    }

    @Override
    protected ErrorReceivedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ErrorReceivedAuditEventEntity((CloudBPMNErrorReceivedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ErrorReceivedAuditEventEntity errorReceivedAuditEventEntity = (ErrorReceivedAuditEventEntity) auditEventEntity;

        return new CloudBPMNErrorReceivedEventImpl(errorReceivedAuditEventEntity.getEventId(),
                                                   errorReceivedAuditEventEntity.getTimestamp(),
                                                   errorReceivedAuditEventEntity.getError(),
                                                   errorReceivedAuditEventEntity.getProcessDefinitionId(),
                                                   errorReceivedAuditEventEntity.getProcessInstanceId());
    }
}
