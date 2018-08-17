package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCompletedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCompletedEventEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskCompletedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskCompletedEvent cloudTaskCompletedEvent = (CloudTaskCompletedEvent) cloudRuntimeEvent;
        TaskCompletedEventEntity taskCompletedEventEntity = new TaskCompletedEventEntity(cloudTaskCompletedEvent.getId(),
                                                                                         cloudTaskCompletedEvent.getTimestamp(),
                                                                                         cloudTaskCompletedEvent.getAppName(),
                                                                                         cloudTaskCompletedEvent.getAppVersion(),
                                                                                         cloudTaskCompletedEvent.getServiceFullName(),
                                                                                         cloudTaskCompletedEvent.getServiceName(),
                                                                                         cloudTaskCompletedEvent.getServiceType(),
                                                                                         cloudTaskCompletedEvent.getServiceVersion(),
                                                                                         cloudTaskCompletedEvent.getEntity());
        return taskCompletedEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        TaskCompletedEventEntity taskCompletedEventEntity = (TaskCompletedEventEntity) auditEventEntity;

        CloudTaskCompletedEventImpl cloudTaskCompletedEvent = new CloudTaskCompletedEventImpl(taskCompletedEventEntity.getEventId(),
                                                                                              taskCompletedEventEntity.getTimestamp(),
                                                                                              taskCompletedEventEntity.getTask());
        cloudTaskCompletedEvent.setAppName(taskCompletedEventEntity.getAppName());
        cloudTaskCompletedEvent.setAppVersion(taskCompletedEventEntity.getAppVersion());
        cloudTaskCompletedEvent.setServiceFullName(taskCompletedEventEntity.getServiceFullName());
        cloudTaskCompletedEvent.setServiceName(taskCompletedEventEntity.getServiceName());
        cloudTaskCompletedEvent.setServiceType(taskCompletedEventEntity.getServiceType());
        cloudTaskCompletedEvent.setServiceVersion(taskCompletedEventEntity.getServiceVersion());

        return cloudTaskCompletedEvent;
    }
}
