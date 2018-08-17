package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCreatedEventEntity;
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
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
