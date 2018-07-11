package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.runtime.api.event.CloudProcessCreated;
import org.activiti.runtime.api.event.CloudProcessStarted;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class ProcessCreatedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessCreated cloudProcessCreated = (CloudProcessCreated) cloudRuntimeEvent;
        ProcessCreatedAuditEventEntity processCreatedAuditEventEntity = new ProcessCreatedAuditEventEntity(cloudProcessCreated.getId(),
                                                                                                           cloudProcessCreated.getTimestamp(),
                                                                                                           cloudProcessCreated.getAppName(),
                                                                                                           cloudProcessCreated.getAppVersion(),
                                                                                                           cloudProcessCreated.getServiceFullName(),
                                                                                                           cloudProcessCreated.getServiceName(),
                                                                                                           cloudProcessCreated.getServiceType(),
                                                                                                           cloudProcessCreated.getServiceVersion(),
                                                                                                           cloudProcessCreated.getEntity());

        return processCreatedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ProcessCreatedAuditEventEntity processCreatedAuditEventEntity = (ProcessCreatedAuditEventEntity) auditEventEntity;
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl(processCreatedAuditEventEntity.getEventId(),
                                                                                                 processCreatedAuditEventEntity.getTimestamp(),
                                                                                                 processCreatedAuditEventEntity.getProcessInstance());
        processCreatedEvent.setAppName(processCreatedAuditEventEntity.getAppName());
        processCreatedEvent.setAppVersion(processCreatedAuditEventEntity.getAppVersion());
        processCreatedEvent.setServiceFullName(processCreatedAuditEventEntity.getServiceFullName());
        processCreatedEvent.setServiceName(processCreatedAuditEventEntity.getServiceName());
        processCreatedEvent.setServiceType(processCreatedAuditEventEntity.getServiceType());
        processCreatedEvent.setServiceVersion(processCreatedAuditEventEntity.getServiceVersion());
        return processCreatedEvent;
    }
}
