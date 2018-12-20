package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessCreatedEventConverter  extends BaseEventToEntityConverter {

    public ProcessCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name();
    }

    @Override
    protected ProcessCreatedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessCreatedEvent cloudProcessCreated = (CloudProcessCreatedEvent) cloudRuntimeEvent;
        return new ProcessCreatedAuditEventEntity(cloudProcessCreated.getId(),
                                                                                        cloudProcessCreated.getTimestamp(),
                                                                                        cloudProcessCreated.getAppName(),
                                                                                        cloudProcessCreated.getAppVersion(),
                                                                                        cloudProcessCreated.getServiceName(),
                                                                                        cloudProcessCreated.getServiceFullName(),
                                                                                        cloudProcessCreated.getServiceType(),
                                                                                        cloudProcessCreated.getServiceVersion(),
                                                                                        cloudProcessCreated.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
