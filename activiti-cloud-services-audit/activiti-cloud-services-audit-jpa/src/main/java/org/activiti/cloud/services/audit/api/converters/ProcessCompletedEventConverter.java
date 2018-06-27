package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.events.ProcessCompletedEventEntity;
import org.activiti.runtime.api.event.CloudProcessCompletedEvent;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudProcessCompletedEventImpl;
import org.activiti.runtime.api.model.ProcessInstance;
import org.springframework.stereotype.Component;

@Component
public class ProcessCompletedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudProcessCompletedEvent cloudProcessCompletedEvent = (CloudProcessCompletedEvent) cloudRuntimeEvent;
        ProcessCompletedEventEntity processCompletedEventEntity = new ProcessCompletedEventEntity(cloudProcessCompletedEvent.getId(),
                                                                                                  cloudProcessCompletedEvent.getTimestamp(),
                                                                                                  cloudProcessCompletedEvent.getAppName(),
                                                                                                  cloudProcessCompletedEvent.getAppVersion(),
                                                                                                  cloudProcessCompletedEvent.getServiceFullName(),
                                                                                                  cloudProcessCompletedEvent.getServiceName(),
                                                                                                  cloudProcessCompletedEvent.getServiceType(),
                                                                                                  cloudProcessCompletedEvent.getServiceVersion(),
                                                                                                  cloudProcessCompletedEvent.getEntity());

        return processCompletedEventEntity;
    }

    @Override
    public CloudRuntimeEvent<ProcessInstance, ProcessRuntimeEvent.ProcessEvents> convertToAPI(AuditEventEntity auditEventEntity) {
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
