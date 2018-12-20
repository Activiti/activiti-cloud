package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessResumedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessResumedAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessResumedEventConverter extends BaseEventToEntityConverter {

    public ProcessResumedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED.name();
    }

    @Override
    protected ProcessResumedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessResumedEvent cloudProcessResumed = (CloudProcessResumedEvent) cloudRuntimeEvent;
       
        return new ProcessResumedAuditEventEntity(cloudProcessResumed.getId(),
                                                  cloudProcessResumed.getTimestamp(),
                                                  cloudProcessResumed.getAppName(),
                                                  cloudProcessResumed.getAppVersion(),
                                                  cloudProcessResumed.getServiceName(),
                                                  cloudProcessResumed.getServiceFullName(),
                                                  cloudProcessResumed.getServiceType(),
                                                  cloudProcessResumed.getServiceVersion(),
                                                  cloudProcessResumed.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
