package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCancelledEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCancelledEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCancelledEventImpl;
import org.springframework.stereotype.Component;

@Component
public class TaskCancelledEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskCancelledEvent cloudTaskCancelledEvent = (CloudTaskCancelledEvent) cloudRuntimeEvent;
        TaskCancelledEventEntity taskCancelledEventEntity = new TaskCancelledEventEntity(cloudTaskCancelledEvent.getId(),
                                                                                         cloudTaskCancelledEvent.getTimestamp(),
                                                                                         cloudTaskCancelledEvent.getAppName(),
                                                                                         cloudTaskCancelledEvent.getAppVersion(),
                                                                                         cloudTaskCancelledEvent.getServiceFullName(),
                                                                                         cloudTaskCancelledEvent.getServiceName(),
                                                                                         cloudTaskCancelledEvent.getServiceType(),
                                                                                         cloudTaskCancelledEvent.getServiceVersion(),
                                                                                         cloudTaskCancelledEvent.getEntity(),
                                                                                         cloudTaskCancelledEvent.getCause());
        return taskCancelledEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
        TaskCancelledEventEntity taskCancelledEventEntity = (TaskCancelledEventEntity) auditEventEntity;

        CloudTaskCancelledEventImpl cloudTaskCancelledEvent = new CloudTaskCancelledEventImpl(taskCancelledEventEntity.getEventId(),
                                                                                              taskCancelledEventEntity.getTimestamp(),
                                                                                              taskCancelledEventEntity.getTask());
        cloudTaskCancelledEvent.setAppName(taskCancelledEventEntity.getAppName());
        cloudTaskCancelledEvent.setAppVersion(taskCancelledEventEntity.getAppVersion());
        cloudTaskCancelledEvent.setServiceFullName(taskCancelledEventEntity.getServiceFullName());
        cloudTaskCancelledEvent.setServiceName(taskCancelledEventEntity.getServiceName());
        cloudTaskCancelledEvent.setServiceType(taskCancelledEventEntity.getServiceType());
        cloudTaskCancelledEvent.setServiceVersion(taskCancelledEventEntity.getServiceVersion());
        cloudTaskCancelledEvent.setCause(taskCancelledEventEntity.getCause());

        return cloudTaskCancelledEvent;
    }
}
