package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessCancelledEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessCancelledEvent cloudProcessCancelledEvent = (CloudProcessCancelledEvent) cloudRuntimeEvent;
        ProcessCancelledAuditEventEntity processCancelledEventEntity = new ProcessCancelledAuditEventEntity(cloudProcessCancelledEvent.getId(),
                                                                                                            cloudProcessCancelledEvent.getTimestamp(),
                                                                                                            cloudProcessCancelledEvent.getCause());
        processCancelledEventEntity.setAppName(cloudProcessCancelledEvent.getAppName());
        processCancelledEventEntity.setAppVersion(cloudProcessCancelledEvent.getAppVersion());
        processCancelledEventEntity.setServiceFullName(cloudProcessCancelledEvent.getServiceFullName());
        processCancelledEventEntity.setServiceName(cloudProcessCancelledEvent.getServiceName());
        processCancelledEventEntity.setServiceType(cloudProcessCancelledEvent.getServiceType());
        processCancelledEventEntity.setServiceVersion(cloudProcessCancelledEvent.getServiceVersion());
        processCancelledEventEntity.setProcessInstance(cloudProcessCancelledEvent.getEntity());
        processCancelledEventEntity.setProcessDefinitionId(cloudProcessCancelledEvent.getEntity().getProcessDefinitionId());
        processCancelledEventEntity.setProcessInstanceId(cloudProcessCancelledEvent.getEntity().getId());

        return processCancelledEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
