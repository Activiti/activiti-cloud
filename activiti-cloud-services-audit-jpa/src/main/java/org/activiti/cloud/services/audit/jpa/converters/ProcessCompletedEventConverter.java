package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;

public class ProcessCompletedEventConverter  extends BaseEventToEntityConverter {
    
    public ProcessCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name();
    }

    @Override
    protected ProcessCompletedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessCompletedEvent cloudProcessCompletedEvent = (CloudProcessCompletedEvent) cloudRuntimeEvent;
        return new ProcessCompletedEventEntity(cloudProcessCompletedEvent.getId(),
                                               cloudProcessCompletedEvent.getTimestamp(),
                                               cloudProcessCompletedEvent.getAppName(),
                                               cloudProcessCompletedEvent.getAppVersion(),
                                               cloudProcessCompletedEvent.getServiceName(),
                                               cloudProcessCompletedEvent.getServiceFullName(),
                                               cloudProcessCompletedEvent.getServiceType(),
                                               cloudProcessCompletedEvent.getServiceVersion(),
                                               cloudProcessCompletedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ProcessCompletedEventEntity processCompletedEventEntity = (ProcessCompletedEventEntity) auditEventEntity;
        CloudProcessCompletedEventImpl cloudProcessCompletedEvent = new CloudProcessCompletedEventImpl(processCompletedEventEntity.getEventId(),
                                                                                                       processCompletedEventEntity.getTimestamp(),
                                                                                                       processCompletedEventEntity.getProcessInstance());
        cloudProcessCompletedEvent.setAppName(processCompletedEventEntity.getAppName());
        cloudProcessCompletedEvent.setAppVersion(processCompletedEventEntity.getAppVersion());
        cloudProcessCompletedEvent.setServiceFullName(processCompletedEventEntity.getServiceFullName());
        cloudProcessCompletedEvent.setServiceName(processCompletedEventEntity.getServiceName());
        cloudProcessCompletedEvent.setServiceType(processCompletedEventEntity.getServiceType());
        cloudProcessCompletedEvent.setServiceVersion(processCompletedEventEntity.getServiceVersion());
        return cloudProcessCompletedEvent;
    }
}
