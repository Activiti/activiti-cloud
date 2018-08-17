package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
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
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
