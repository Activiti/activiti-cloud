package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCancelledEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCancelledEventEntity;

public class TaskCancelledEventConverter extends BaseEventToEntityConverter {

    public TaskCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name();
    }

    @Override
    protected TaskCancelledEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TaskCancelledEventEntity((CloudTaskCancelledEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskCancelledEventEntity taskCancelledEventEntity = (TaskCancelledEventEntity) auditEventEntity;

        return new CloudTaskCancelledEventImpl(taskCancelledEventEntity.getEventId(),
                                               taskCancelledEventEntity.getTimestamp(),
                                               taskCancelledEventEntity.getTask());
    }
}
