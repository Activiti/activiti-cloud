package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.ActivityCompletedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.stereotype.Component;

@Component
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
        CloudBPMNActivityCompletedEvent cloudBPMNActivityCompletedEvent = (CloudBPMNActivityCompletedEvent) cloudRuntimeEvent;

        return new ActivityCompletedAuditEventEntity(cloudBPMNActivityCompletedEvent.getId(),
                                                     cloudBPMNActivityCompletedEvent.getTimestamp(),
                                                     cloudBPMNActivityCompletedEvent.getAppName(),
                                                     cloudBPMNActivityCompletedEvent.getAppVersion(),
                                                     cloudBPMNActivityCompletedEvent.getServiceFullName(),
                                                     cloudBPMNActivityCompletedEvent.getServiceName(),
                                                     cloudBPMNActivityCompletedEvent.getServiceType(),
                                                     cloudBPMNActivityCompletedEvent.getServiceVersion(),
                                                     cloudBPMNActivityCompletedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
  
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
