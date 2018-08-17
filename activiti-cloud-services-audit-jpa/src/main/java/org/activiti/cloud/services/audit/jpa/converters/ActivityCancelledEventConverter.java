package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.ActivityCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class   ActivityCancelledEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudBPMNActivityCancelledEvent cloudBPMNActivityCancelledEvent = (CloudBPMNActivityCancelledEvent) cloudRuntimeEvent;
        ActivityCancelledAuditEventEntity activityCancelledAuditEventEntity = new ActivityCancelledAuditEventEntity(cloudBPMNActivityCancelledEvent.getId(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getTimestamp(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getAppName(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getAppVersion(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getServiceFullName(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getServiceName(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getServiceType(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getServiceVersion(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getEntity(),
                                                                                                                    cloudBPMNActivityCancelledEvent.getCause());
        activityCancelledAuditEventEntity.setEntityId(cloudBPMNActivityCancelledEvent.getProcessInstanceId());
        activityCancelledAuditEventEntity.setProcessDefinitionId(cloudBPMNActivityCancelledEvent.getProcessDefinitionId());
        activityCancelledAuditEventEntity.setProcessInstanceId(cloudBPMNActivityCancelledEvent.getProcessInstanceId());
        return activityCancelledAuditEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        ActivityCancelledAuditEventEntity activityCancelledAuditEventEntity = (ActivityCancelledAuditEventEntity) auditEventEntity;

        CloudBPMNActivityCancelledEventImpl bpmnActivityCancelledEvent = new CloudBPMNActivityCancelledEventImpl(activityCancelledAuditEventEntity.getEventId(),
                                                                                                                 activityCancelledAuditEventEntity.getTimestamp(),
                                                                                                                 activityCancelledAuditEventEntity.getBpmnActivity(),
                                                                                                                 activityCancelledAuditEventEntity.getProcessDefinitionId(),
                                                                                                                 activityCancelledAuditEventEntity.getProcessInstanceId(),
                                                                                                                 activityCancelledAuditEventEntity.getCause());
        bpmnActivityCancelledEvent.setAppName(activityCancelledAuditEventEntity.getAppName());
        bpmnActivityCancelledEvent.setAppVersion(activityCancelledAuditEventEntity.getAppVersion());
        bpmnActivityCancelledEvent.setServiceFullName(activityCancelledAuditEventEntity.getServiceFullName());
        bpmnActivityCancelledEvent.setServiceName(activityCancelledAuditEventEntity.getServiceName());
        bpmnActivityCancelledEvent.setServiceType(activityCancelledAuditEventEntity.getServiceType());
        bpmnActivityCancelledEvent.setServiceVersion(activityCancelledAuditEventEntity.getServiceVersion());

        return bpmnActivityCancelledEvent;
    }
}
