package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessSuspendedAuditEventEntity;
import org.activiti.runtime.api.event.CloudProcessSuspended;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessSuspendedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class ProcessSuspendedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessSuspended cloudProcessSuspended = (CloudProcessSuspended) cloudRuntimeEvent;
        ProcessSuspendedAuditEventEntity processSuspendedAuditEventEntity = new ProcessSuspendedAuditEventEntity(cloudProcessSuspended.getId(),
                                                                                                                 cloudProcessSuspended.getTimestamp());
        processSuspendedAuditEventEntity.setAppName(cloudProcessSuspended.getAppName());
        processSuspendedAuditEventEntity.setAppVersion(cloudProcessSuspended.getAppVersion());
        processSuspendedAuditEventEntity.setServiceFullName(cloudProcessSuspended.getServiceFullName());
        processSuspendedAuditEventEntity.setServiceName(cloudProcessSuspended.getServiceName());
        processSuspendedAuditEventEntity.setServiceType(cloudProcessSuspended.getServiceType());
        processSuspendedAuditEventEntity.setServiceVersion(cloudProcessSuspended.getServiceVersion());
        processSuspendedAuditEventEntity.setProcessDefinitionId(cloudProcessSuspended.getEntity().getProcessDefinitionId());
        processSuspendedAuditEventEntity.setProcessInstanceId(cloudProcessSuspended.getEntity().getId());

        return processSuspendedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
