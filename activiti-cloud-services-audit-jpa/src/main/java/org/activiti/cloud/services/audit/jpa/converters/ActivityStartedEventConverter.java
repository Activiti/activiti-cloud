package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;

public class ActivityStartedEventConverter extends BaseEventToEntityConverter {

    public ActivityStartedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED.name();
    }

    @Override
    protected ActivityStartedAuditEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new ActivityStartedAuditEventEntity((CloudBPMNActivityStartedEvent) cloudRuntimeEvent);           
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        ActivityStartedAuditEventEntity activityStartedAuditEventEntity = (ActivityStartedAuditEventEntity) auditEventEntity;

        return new CloudBPMNActivityStartedEventImpl(activityStartedAuditEventEntity.getEventId(),
                                                     activityStartedAuditEventEntity.getTimestamp(),
                                                     activityStartedAuditEventEntity.getBpmnActivity(),
                                                     activityStartedAuditEventEntity.getProcessDefinitionId(),
                                                     activityStartedAuditEventEntity.getProcessInstanceId());
    }
}
