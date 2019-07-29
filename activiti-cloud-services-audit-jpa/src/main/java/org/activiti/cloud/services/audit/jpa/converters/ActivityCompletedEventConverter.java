package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class ActivityCompletedEventConverter extends BaseEventToEntityConverter {

    public ActivityCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name();
    }

    @Override
    protected ActivityCompletedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ActivityCompletedAuditEventEntity((CloudBPMNActivityCompletedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
  
        ActivityCompletedAuditEventEntity activityCompletedAuditEventEntity = (ActivityCompletedAuditEventEntity) auditEventEntity;

        return new CloudBPMNActivityCompletedEventImpl(activityCompletedAuditEventEntity.getEventId(),
                                                       activityCompletedAuditEventEntity.getTimestamp(),
                                                       activityCompletedAuditEventEntity.getBpmnActivity(),
                                                       activityCompletedAuditEventEntity.getProcessDefinitionId(),
                                                       activityCompletedAuditEventEntity.getProcessInstanceId());
    }
}
