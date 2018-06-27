package org.activiti.cloud.services.audit.api.converters;

import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.events.TaskCreatedEventEntity;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.CloudTaskCreatedEvent;
import org.activiti.runtime.api.event.TaskRuntimeEvent;
import org.activiti.runtime.api.event.impl.CloudTaskCreatedEventImpl;
import org.activiti.runtime.api.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskCreatedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CREATED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskCreatedEvent cloudTaskCreatedEvent = (CloudTaskCreatedEvent) cloudRuntimeEvent;
        TaskCreatedEventEntity taskCreatedEventEntity = new TaskCreatedEventEntity(cloudTaskCreatedEvent.getId(),
                                                                                   cloudTaskCreatedEvent.getTimestamp(),
                                                                                   cloudTaskCreatedEvent.getAppName(),
                                                                                   cloudTaskCreatedEvent.getAppVersion(),
                                                                                   cloudTaskCreatedEvent.getServiceFullName(),
                                                                                   cloudTaskCreatedEvent.getServiceName(),
                                                                                   cloudTaskCreatedEvent.getServiceType(),
                                                                                   cloudTaskCreatedEvent.getServiceVersion(),
                                                                                   cloudTaskCreatedEvent.getEntity());
        return taskCreatedEventEntity;
    }

    @Override
    public CloudRuntimeEvent<Task, TaskRuntimeEvent.TaskEvents> convertToAPI(AuditEventEntity auditEventEntity) {
        TaskCreatedEventEntity taskCreatedEventEntity = (TaskCreatedEventEntity) auditEventEntity;

        CloudTaskCreatedEventImpl cloudTaskCreatedEvent = new CloudTaskCreatedEventImpl(taskCreatedEventEntity.getEventId(),
                                                                                        taskCreatedEventEntity.getTimestamp(),
                                                                                        taskCreatedEventEntity.getTask());
        cloudTaskCreatedEvent.setAppName(taskCreatedEventEntity.getAppName());
        cloudTaskCreatedEvent.setAppVersion(taskCreatedEventEntity.getAppVersion());
        cloudTaskCreatedEvent.setServiceFullName(taskCreatedEventEntity.getServiceFullName());
        cloudTaskCreatedEvent.setServiceName(taskCreatedEventEntity.getServiceName());
        cloudTaskCreatedEvent.setServiceType(taskCreatedEventEntity.getServiceType());
        cloudTaskCreatedEvent.setServiceVersion(taskCreatedEventEntity.getServiceVersion());

        return cloudTaskCreatedEvent;
    }
}
