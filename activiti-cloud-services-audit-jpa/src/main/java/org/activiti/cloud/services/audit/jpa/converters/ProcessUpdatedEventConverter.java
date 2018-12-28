package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessUpdatedAuditEventEntity;

public class ProcessUpdatedEventConverter extends BaseEventToEntityConverter {
    
    public ProcessUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED.name();
    }

    @Override
    protected ProcessUpdatedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessUpdatedEvent cloudProcessUpdated = (CloudProcessUpdatedEvent) cloudRuntimeEvent;
         
        return new ProcessUpdatedAuditEventEntity(cloudProcessUpdated.getId(),
                                                  cloudProcessUpdated.getTimestamp(),
                                                  cloudProcessUpdated.getAppName(),
                                                  cloudProcessUpdated.getAppVersion(),
                                                  cloudProcessUpdated.getServiceName(),
                                                  cloudProcessUpdated.getServiceFullName(),
                                                  cloudProcessUpdated.getServiceType(),
                                                  cloudProcessUpdated.getServiceVersion(),
                                                  cloudProcessUpdated.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
