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
        CloudProcessCancelledEvent cloudProcessCancelledEvent = (CloudProcessCancelledEvent) cloudRuntimeEvent;
        return new ProcessCancelledAuditEventEntity(cloudProcessCancelledEvent.getId(),
                                                    cloudProcessCancelledEvent.getTimestamp(),
                                                    cloudProcessCancelledEvent.getAppName(),
                                                    cloudProcessCancelledEvent.getAppVersion(),
                                                    cloudProcessCancelledEvent.getServiceName(),
                                                    cloudProcessCancelledEvent.getServiceFullName(),
                                                    cloudProcessCancelledEvent.getServiceType(),
                                                    cloudProcessCancelledEvent.getServiceVersion(),
                                                    cloudProcessCancelledEvent.getEntity(),
                                                    cloudProcessCancelledEvent.getCause()); 
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessCancelledAuditEventEntity processCancelledAuditEventEntity = (ProcessCancelledAuditEventEntity) auditEventEntity;
        CloudProcessCancelledEventImpl cloudProcessCancelledEvent = new CloudProcessCancelledEventImpl(processCancelledAuditEventEntity.getEventId(),
                                                                                                       processCancelledAuditEventEntity.getTimestamp(),
                                                                                                       processCancelledAuditEventEntity.getProcessInstance());
        cloudProcessCancelledEvent.setAppName(processCancelledAuditEventEntity.getAppName());
        cloudProcessCancelledEvent.setAppVersion(processCancelledAuditEventEntity.getAppVersion());
        cloudProcessCancelledEvent.setServiceFullName(processCancelledAuditEventEntity.getServiceFullName());
        cloudProcessCancelledEvent.setServiceName(processCancelledAuditEventEntity.getServiceName());
        cloudProcessCancelledEvent.setServiceType(processCancelledAuditEventEntity.getServiceType());
        cloudProcessCancelledEvent.setServiceVersion(processCancelledAuditEventEntity.getServiceVersion());
        cloudProcessCancelledEvent.setCause(processCancelledAuditEventEntity.getCause());
        return cloudProcessCancelledEvent;
    }
}
