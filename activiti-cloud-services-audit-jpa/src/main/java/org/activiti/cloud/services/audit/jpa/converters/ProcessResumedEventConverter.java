package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessResumedAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessResumedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessResumedEvent cloudProcessResumed = (CloudProcessResumedEvent) cloudRuntimeEvent;
        ProcessResumedAuditEventEntity processResumedAuditEventEntity = new ProcessResumedAuditEventEntity(cloudProcessResumed.getId(),
                                                                                                             cloudProcessResumed.getTimestamp());
        processResumedAuditEventEntity.setAppName(cloudProcessResumed.getAppName());
        processResumedAuditEventEntity.setAppVersion(cloudProcessResumed.getAppVersion());
        processResumedAuditEventEntity.setServiceFullName(cloudProcessResumed.getServiceFullName());
        processResumedAuditEventEntity.setServiceName(cloudProcessResumed.getServiceName());
        processResumedAuditEventEntity.setServiceType(cloudProcessResumed.getServiceType());
        processResumedAuditEventEntity.setServiceVersion(cloudProcessResumed.getServiceVersion());
        processResumedAuditEventEntity.setProcessDefinitionId(cloudProcessResumed.getEntity().getProcessDefinitionId());
        processResumedAuditEventEntity.setProcessInstanceId(cloudProcessResumed.getEntity().getId());
        processResumedAuditEventEntity.setProcessInstance(cloudProcessResumed.getEntity());

        return processResumedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ProcessResumedAuditEventEntity processResumedAuditEventEntity = (ProcessResumedAuditEventEntity) auditEventEntity;
        CloudProcessResumedEventImpl cloudProcessResumedEvent = new CloudProcessResumedEventImpl(processResumedAuditEventEntity.getEventId(),
                                                                                                 processResumedAuditEventEntity.getTimestamp(),
                                                                                                 processResumedAuditEventEntity.getProcessInstance());
        cloudProcessResumedEvent.setAppName(processResumedAuditEventEntity.getAppName());
        cloudProcessResumedEvent.setAppVersion(processResumedAuditEventEntity.getAppVersion());
        cloudProcessResumedEvent.setServiceFullName(processResumedAuditEventEntity.getServiceFullName());
        cloudProcessResumedEvent.setServiceName(processResumedAuditEventEntity.getServiceName());
        cloudProcessResumedEvent.setServiceType(processResumedAuditEventEntity.getServiceType());
        cloudProcessResumedEvent.setServiceVersion(processResumedAuditEventEntity.getServiceVersion());

        return cloudProcessResumedEvent;
    }
}
