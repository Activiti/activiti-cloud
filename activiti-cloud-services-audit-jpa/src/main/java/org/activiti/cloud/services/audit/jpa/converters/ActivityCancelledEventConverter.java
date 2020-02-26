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
        return new ActivityCancelledAuditEventEntity((CloudBPMNActivityCancelledEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ActivityCancelledAuditEventEntity activityCancelledAuditEventEntity = (ActivityCancelledAuditEventEntity) auditEventEntity;

        return new CloudBPMNActivityCancelledEventImpl(activityCancelledAuditEventEntity.getEventId(),
                                                       activityCancelledAuditEventEntity.getTimestamp(),
                                                       activityCancelledAuditEventEntity.getBpmnActivity(),
                                                       activityCancelledAuditEventEntity.getProcessDefinitionId(),
                                                       activityCancelledAuditEventEntity.getProcessInstanceId(),
                                                       activityCancelledAuditEventEntity.getCause());
    }
}
