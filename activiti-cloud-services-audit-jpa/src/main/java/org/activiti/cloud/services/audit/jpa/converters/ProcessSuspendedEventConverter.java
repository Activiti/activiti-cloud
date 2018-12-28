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
        CloudProcessSuspendedEvent cloudProcessSuspended = (CloudProcessSuspendedEvent) cloudRuntimeEvent;
                 
        return new ProcessSuspendedAuditEventEntity(cloudProcessSuspended.getId(),
                                                    cloudProcessSuspended.getTimestamp(),
                                                    cloudProcessSuspended.getAppName(),
                                                    cloudProcessSuspended.getAppVersion(),
                                                    cloudProcessSuspended.getServiceName(),
                                                    cloudProcessSuspended.getServiceFullName(),
                                                    cloudProcessSuspended.getServiceType(),
                                                    cloudProcessSuspended.getServiceVersion(),
                                                    cloudProcessSuspended.getEntity());
 
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessSuspendedAuditEventEntity processSuspendedAuditEventEntity = (ProcessSuspendedAuditEventEntity) auditEventEntity;
        CloudProcessSuspendedEventImpl cloudProcessSuspendedEvent = new CloudProcessSuspendedEventImpl(processSuspendedAuditEventEntity.getEventId(),
                                                                                                       processSuspendedAuditEventEntity.getTimestamp(),
                                                                                                       processSuspendedAuditEventEntity.getProcessInstance());
        cloudProcessSuspendedEvent.setAppName(processSuspendedAuditEventEntity.getAppName());
        cloudProcessSuspendedEvent.setAppVersion(processSuspendedAuditEventEntity.getAppVersion());
        cloudProcessSuspendedEvent.setServiceFullName(processSuspendedAuditEventEntity.getServiceFullName());
        cloudProcessSuspendedEvent.setServiceName(processSuspendedAuditEventEntity.getServiceName());
        cloudProcessSuspendedEvent.setServiceType(processSuspendedAuditEventEntity.getServiceType());
        cloudProcessSuspendedEvent.setServiceVersion(processSuspendedAuditEventEntity.getServiceVersion());

        return cloudProcessSuspendedEvent;
    }
}
