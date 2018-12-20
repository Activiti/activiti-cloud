package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCreatedEventEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskCreatedEventConverter extends BaseEventToEntityConverter {
    
    public TaskCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CREATED.name();
    }

    @Override
    protected TaskCreatedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskCreatedEvent cloudTaskCreatedEvent = (CloudTaskCreatedEvent) cloudRuntimeEvent;       
        return new TaskCreatedEventEntity(cloudTaskCreatedEvent.getId(),
                                          cloudTaskCreatedEvent.getTimestamp(),
                                          cloudTaskCreatedEvent.getAppName(),
                                          cloudTaskCreatedEvent.getAppVersion(),
                                          cloudTaskCreatedEvent.getServiceName(),
                                          cloudTaskCreatedEvent.getServiceFullName(),
                                          cloudTaskCreatedEvent.getServiceType(),
                                          cloudTaskCreatedEvent.getServiceVersion(),
                                          cloudTaskCreatedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
