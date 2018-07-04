package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.runtime.api.event.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class ProcessStartedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessStartedEvent cloudProcessStartedEvent = (CloudProcessStartedEvent) cloudRuntimeEvent;
        ProcessStartedAuditEventEntity processStartedEventEntity = new ProcessStartedAuditEventEntity(cloudProcessStartedEvent.getId(),
                                                                                                      cloudProcessStartedEvent.getTimestamp(),
                                                                                                      cloudProcessStartedEvent.getAppName(),
                                                                                                      cloudProcessStartedEvent.getAppVersion(),
                                                                                                      cloudProcessStartedEvent.getServiceFullName(),
                                                                                                      cloudProcessStartedEvent.getServiceName(),
                                                                                                      cloudProcessStartedEvent.getServiceType(),
                                                                                                      cloudProcessStartedEvent.getServiceVersion(),
                                                                                                      cloudProcessStartedEvent.getEntity(),
                                                                                                      cloudProcessStartedEvent.getNestedProcessDefinitionId(),
                                                                                                      cloudProcessStartedEvent.getNestedProcessInstanceId());

        return processStartedEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
