package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCancelledEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskSuspendedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCancelledEvent;
import org.activiti.runtime.api.event.CloudTaskSuspendedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCancelledEventImpl;
import org.activiti.runtime.api.event.impl.CloudTaskSuspendedEventImpl;
import org.springframework.stereotype.Component;

@Component
public class TaskSuspendedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskSuspendedEvent cloudTaskSuspendedEvent = (CloudTaskSuspendedEvent) cloudRuntimeEvent;
        TaskSuspendedEventEntity taskSuspendedEventEntity = new TaskSuspendedEventEntity(cloudTaskSuspendedEvent.getId(),
                                                                                         cloudTaskSuspendedEvent.getTimestamp(),
                                                                                         cloudTaskSuspendedEvent.getAppName(),
                                                                                         cloudTaskSuspendedEvent.getAppVersion(),
                                                                                         cloudTaskSuspendedEvent.getServiceFullName(),
                                                                                         cloudTaskSuspendedEvent.getServiceName(),
                                                                                         cloudTaskSuspendedEvent.getServiceType(),
                                                                                         cloudTaskSuspendedEvent.getServiceVersion(),
                                                                                         cloudTaskSuspendedEvent.getEntity());
        return taskSuspendedEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        TaskSuspendedEventEntity taskSuspendedEventEntity = (TaskSuspendedEventEntity) auditEventEntity;

        CloudTaskSuspendedEventImpl cloudTaskSuspendedEvent = new CloudTaskSuspendedEventImpl(taskSuspendedEventEntity.getEventId(),
                                                                                              taskSuspendedEventEntity.getTimestamp(),
                                                                                              taskSuspendedEventEntity.getTask());
        cloudTaskSuspendedEvent.setAppName(taskSuspendedEventEntity.getAppName());
        cloudTaskSuspendedEvent.setAppVersion(taskSuspendedEventEntity.getAppVersion());
        cloudTaskSuspendedEvent.setServiceFullName(taskSuspendedEventEntity.getServiceFullName());
        cloudTaskSuspendedEvent.setServiceName(taskSuspendedEventEntity.getServiceName());
        cloudTaskSuspendedEvent.setServiceType(taskSuspendedEventEntity.getServiceType());
        cloudTaskSuspendedEvent.setServiceVersion(taskSuspendedEventEntity.getServiceVersion());

        return cloudTaskSuspendedEvent;
    }
}
