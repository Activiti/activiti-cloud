package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.runtime.api.event.BPMNActivityEvent;
import org.activiti.runtime.api.event.CloudBPMNActivityCompleted;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudBPMNActivityCompletedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class ActivityCompletedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudBPMNActivityCompleted cloudBPMNActivityCompletedEvent = (CloudBPMNActivityCompleted) cloudRuntimeEvent;
        ActivityCompletedAuditEventEntity activityCompletedAuditEventEntity = new ActivityCompletedAuditEventEntity(cloudBPMNActivityCompletedEvent.getId(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getTimestamp(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getAppName(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getAppVersion(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getServiceFullName(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getServiceName(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getServiceType(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getServiceVersion(),
                                                                                                                    cloudBPMNActivityCompletedEvent.getEntity());
        activityCompletedAuditEventEntity.setEntityId(cloudBPMNActivityCompletedEvent.getProcessInstanceId());
        activityCompletedAuditEventEntity.setProcessDefinitionId(cloudBPMNActivityCompletedEvent.getProcessDefinitionId());
        activityCompletedAuditEventEntity.setProcessInstanceId(cloudBPMNActivityCompletedEvent.getProcessInstanceId());
        return activityCompletedAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ActivityCompletedAuditEventEntity activityCompletedAuditEventEntity = (ActivityCompletedAuditEventEntity) auditEventEntity;

        CloudBPMNActivityCompletedEventImpl bpmnActivityCompletedEvent = new CloudBPMNActivityCompletedEventImpl(activityCompletedAuditEventEntity.getEventId(),
                                                                                                                 activityCompletedAuditEventEntity.getTimestamp(),
                                                                                                                 activityCompletedAuditEventEntity.getBpmnActivity(),
                                                                                                                 activityCompletedAuditEventEntity.getProcessDefinitionId(),
                                                                                                                 activityCompletedAuditEventEntity.getProcessInstanceId());
        bpmnActivityCompletedEvent.setAppName(activityCompletedAuditEventEntity.getAppName());
        bpmnActivityCompletedEvent.setAppVersion(activityCompletedAuditEventEntity.getAppVersion());
        bpmnActivityCompletedEvent.setServiceFullName(activityCompletedAuditEventEntity.getServiceFullName());
        bpmnActivityCompletedEvent.setServiceName(activityCompletedAuditEventEntity.getServiceName());
        bpmnActivityCompletedEvent.setServiceType(activityCompletedAuditEventEntity.getServiceType());
        bpmnActivityCompletedEvent.setServiceVersion(activityCompletedAuditEventEntity.getServiceVersion());
        return bpmnActivityCompletedEvent;
    }
}
