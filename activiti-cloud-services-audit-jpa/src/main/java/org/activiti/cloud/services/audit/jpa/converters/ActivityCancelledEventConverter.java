package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class ActivityCancelledEventConverter extends BaseEventToEntityConverter {

    public ActivityCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }

    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED.name();
    }

    @Override
    protected ActivityCancelledAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudBPMNActivityCancelledEvent cloudBPMNActivityCancelledEvent = (CloudBPMNActivityCancelledEvent) cloudRuntimeEvent;
        return new ActivityCancelledAuditEventEntity(cloudBPMNActivityCancelledEvent.getId(),
                                                     cloudBPMNActivityCancelledEvent.getTimestamp(),
                                                     cloudBPMNActivityCancelledEvent.getAppName(),
                                                     cloudBPMNActivityCancelledEvent.getAppVersion(),
                                                     cloudBPMNActivityCancelledEvent.getServiceFullName(),
                                                     cloudBPMNActivityCancelledEvent.getServiceName(),
                                                     cloudBPMNActivityCancelledEvent.getServiceType(),
                                                     cloudBPMNActivityCancelledEvent.getServiceVersion(),
                                                     cloudBPMNActivityCancelledEvent.getEntity(),
                                                     cloudBPMNActivityCancelledEvent.getCause());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
