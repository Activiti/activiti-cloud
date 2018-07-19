package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.ActivityStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.runtime.api.event.BPMNActivityEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityStarted;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityStartedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class ActivityStartedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudBPMNActivityStarted cloudActivityStartedEvent = (CloudBPMNActivityStarted) cloudRuntimeEvent;
        ActivityStartedAuditEventEntity activityStartedAuditEventEntity = new ActivityStartedAuditEventEntity(cloudActivityStartedEvent.getId(),
                                                                                                              cloudActivityStartedEvent.getTimestamp(),
                                                                                                              cloudActivityStartedEvent.getAppName(),
                                                                                                              cloudActivityStartedEvent.getAppVersion(),
                                                                                                              cloudActivityStartedEvent.getServiceFullName(),
                                                                                                              cloudActivityStartedEvent.getServiceName(),
                                                                                                              cloudActivityStartedEvent.getServiceType(),
                                                                                                              cloudActivityStartedEvent.getServiceVersion(),
                                                                                                              cloudActivityStartedEvent.getEntity());
        activityStartedAuditEventEntity.setEntityId(cloudActivityStartedEvent.getProcessInstanceId());
        activityStartedAuditEventEntity.setProcessDefinitionId(cloudActivityStartedEvent.getProcessDefinitionId());
        activityStartedAuditEventEntity.setProcessInstanceId(cloudActivityStartedEvent.getProcessInstanceId());
        return activityStartedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ActivityStartedAuditEventEntity activityStartedAuditEventEntity = (ActivityStartedAuditEventEntity) auditEventEntity;

        CloudBPMNActivityStartedEventImpl bpmnActivityStartedEvent = new CloudBPMNActivityStartedEventImpl(activityStartedAuditEventEntity.getEventId(),
                                                                                                           activityStartedAuditEventEntity.getTimestamp(),
                                                                                                           activityStartedAuditEventEntity.getBpmnActivity(),
                                                                                                           activityStartedAuditEventEntity.getProcessDefinitionId(),
                                                                                                           activityStartedAuditEventEntity.getProcessInstanceId());
        bpmnActivityStartedEvent.setAppName(activityStartedAuditEventEntity.getAppName());
        bpmnActivityStartedEvent.setAppVersion(activityStartedAuditEventEntity.getAppVersion());
        bpmnActivityStartedEvent.setServiceFullName(activityStartedAuditEventEntity.getServiceFullName());
        bpmnActivityStartedEvent.setServiceName(activityStartedAuditEventEntity.getServiceName());
        bpmnActivityStartedEvent.setServiceType(activityStartedAuditEventEntity.getServiceType());
        bpmnActivityStartedEvent.setServiceVersion(activityStartedAuditEventEntity.getServiceVersion());

        return bpmnActivityStartedEvent;
    }
}
