package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.events.TaskCompletedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCompletedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCompletedEventImpl;
import org.activiti.runtime.api.model.Task;
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
    public CloudRuntimeEvent<Task, TaskRuntimeEvent.TaskEvents> convertToAPI(AuditEventEntity auditEventEntity) {
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
