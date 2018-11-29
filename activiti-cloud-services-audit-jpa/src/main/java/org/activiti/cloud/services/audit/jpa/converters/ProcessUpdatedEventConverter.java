package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessUpdatedAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessUpdatedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessUpdatedEvent cloudProcessUpdated = (CloudProcessUpdatedEvent) cloudRuntimeEvent;
        ProcessUpdatedAuditEventEntity processUpdatedAuditEventEntity = new ProcessUpdatedAuditEventEntity(cloudProcessUpdated.getId(),
                                                                                                           cloudProcessUpdated.getTimestamp());
        processUpdatedAuditEventEntity.setAppName(cloudProcessUpdated.getAppName());
        processUpdatedAuditEventEntity.setAppVersion(cloudProcessUpdated.getAppVersion());
        processUpdatedAuditEventEntity.setServiceFullName(cloudProcessUpdated.getServiceFullName());
        processUpdatedAuditEventEntity.setServiceName(cloudProcessUpdated.getServiceName());
        processUpdatedAuditEventEntity.setServiceType(cloudProcessUpdated.getServiceType());
        processUpdatedAuditEventEntity.setServiceVersion(cloudProcessUpdated.getServiceVersion());
        processUpdatedAuditEventEntity.setProcessDefinitionId(cloudProcessUpdated.getEntity().getProcessDefinitionId());
        processUpdatedAuditEventEntity.setProcessInstanceId(cloudProcessUpdated.getEntity().getId());

        return processUpdatedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ProcessUpdatedAuditEventEntity processUpdatedAuditEventEntity = (ProcessUpdatedAuditEventEntity) auditEventEntity;
        CloudProcessUpdatedEventImpl cloudProcessUpdatedEvent = new CloudProcessUpdatedEventImpl(processUpdatedAuditEventEntity.getEventId(),
                                                                                                 processUpdatedAuditEventEntity.getTimestamp(),
                                                                                                 processUpdatedAuditEventEntity.getProcessInstance());
        cloudProcessUpdatedEvent.setAppName(processUpdatedAuditEventEntity.getAppName());
        cloudProcessUpdatedEvent.setAppVersion(processUpdatedAuditEventEntity.getAppVersion());
        cloudProcessUpdatedEvent.setServiceFullName(processUpdatedAuditEventEntity.getServiceFullName());
        cloudProcessUpdatedEvent.setServiceName(processUpdatedAuditEventEntity.getServiceName());
        cloudProcessUpdatedEvent.setServiceType(processUpdatedAuditEventEntity.getServiceType());
        cloudProcessUpdatedEvent.setServiceVersion(processUpdatedAuditEventEntity.getServiceVersion());

        return cloudProcessUpdatedEvent;
    }
}
