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
        CloudProcessStartedEvent cloudProcessStartedEvent = (CloudProcessStartedEvent) cloudRuntimeEvent;
        return new ProcessStartedAuditEventEntity(cloudProcessStartedEvent.getId(),
                                                  cloudProcessStartedEvent.getTimestamp(),
                                                  cloudProcessStartedEvent.getAppName(),
                                                  cloudProcessStartedEvent.getAppVersion(),
                                                  cloudProcessStartedEvent.getServiceName(),
                                                  cloudProcessStartedEvent.getServiceFullName(),
                                                  cloudProcessStartedEvent.getServiceType(),
                                                  cloudProcessStartedEvent.getServiceVersion(),
                                                  cloudProcessStartedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessStartedAuditEventEntity processStartedAuditEventEntity = (ProcessStartedAuditEventEntity) auditEventEntity;
        CloudProcessStartedEventImpl cloudProcessStartedEvent = new CloudProcessStartedEventImpl(processStartedAuditEventEntity.getEventId(),
                                                                                                 processStartedAuditEventEntity.getTimestamp(),
                                                                                                 processStartedAuditEventEntity.getProcessInstance());
        cloudProcessStartedEvent.setAppName(processStartedAuditEventEntity.getAppName());
        cloudProcessStartedEvent.setAppVersion(processStartedAuditEventEntity.getAppVersion());
        cloudProcessStartedEvent.setServiceFullName(processStartedAuditEventEntity.getServiceFullName());
        cloudProcessStartedEvent.setServiceName(processStartedAuditEventEntity.getServiceName());
        cloudProcessStartedEvent.setServiceType(processStartedAuditEventEntity.getServiceType());
        cloudProcessStartedEvent.setServiceVersion(processStartedAuditEventEntity.getServiceVersion());
        return cloudProcessStartedEvent;
    }
}
