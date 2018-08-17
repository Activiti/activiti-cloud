package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ActivityCompletedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudBPMNActivityCompletedEvent cloudBPMNActivityCompletedEvent = (CloudBPMNActivityCompletedEvent) cloudRuntimeEvent;
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
