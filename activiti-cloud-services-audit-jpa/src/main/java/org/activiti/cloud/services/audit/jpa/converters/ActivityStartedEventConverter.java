package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.stereotype.Component;

@Component
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
        CloudBPMNActivityStartedEvent cloudActivityStartedEvent = (CloudBPMNActivityStartedEvent) cloudRuntimeEvent;
        return new ActivityStartedAuditEventEntity(cloudActivityStartedEvent.getId(),
                                                   cloudActivityStartedEvent.getTimestamp(),
                                                   cloudActivityStartedEvent.getAppName(),
                                                   cloudActivityStartedEvent.getAppVersion(),
                                                   cloudActivityStartedEvent.getServiceFullName(),
                                                   cloudActivityStartedEvent.getServiceName(),
                                                   cloudActivityStartedEvent.getServiceType(),
                                                   cloudActivityStartedEvent.getServiceVersion(),
                                                   cloudActivityStartedEvent.getEntity());
               
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
