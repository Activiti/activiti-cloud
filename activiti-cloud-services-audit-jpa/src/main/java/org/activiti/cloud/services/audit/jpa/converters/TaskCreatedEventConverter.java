package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCreatedEventEntity;

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
        return new TaskCreatedEventEntity((CloudTaskCreatedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskCreatedEventEntity taskCreatedEventEntity = (TaskCreatedEventEntity) auditEventEntity;

        return new CloudTaskCreatedEventImpl(taskCreatedEventEntity.getEventId(),
                                             taskCreatedEventEntity.getTimestamp(),
                                             taskCreatedEventEntity.getTask());
    }
}
