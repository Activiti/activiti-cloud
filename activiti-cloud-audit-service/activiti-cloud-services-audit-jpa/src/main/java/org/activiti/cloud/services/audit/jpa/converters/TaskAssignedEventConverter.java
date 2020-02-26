package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskAssignedEventEntity;

public class TaskAssignedEventConverter extends BaseEventToEntityConverter {

    public TaskAssignedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name();
    }

    @Override
    protected TaskAssignedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        return new TaskAssignedEventEntity((CloudTaskAssignedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskAssignedEventEntity taskAssignedEventEntity = (TaskAssignedEventEntity) auditEventEntity;

        return new CloudTaskAssignedEventImpl(taskAssignedEventEntity.getEventId(),
                                              taskAssignedEventEntity.getTimestamp(),
                                              taskAssignedEventEntity.getTask());
    }
}
