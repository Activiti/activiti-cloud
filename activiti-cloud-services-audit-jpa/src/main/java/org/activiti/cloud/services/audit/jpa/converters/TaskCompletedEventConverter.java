package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCompletedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCompletedEventEntity;

public class TaskCompletedEventConverter extends BaseEventToEntityConverter {
    
    public TaskCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_COMPLETED.name();
    }

    @Override
    protected TaskCompletedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TaskCompletedEventEntity((CloudTaskCompletedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskCompletedEventEntity taskCompletedEventEntity = (TaskCompletedEventEntity) auditEventEntity;

        return new CloudTaskCompletedEventImpl(taskCompletedEventEntity.getEventId(),
                                               taskCompletedEventEntity.getTimestamp(),
                                               taskCompletedEventEntity.getTask());
    }
}
