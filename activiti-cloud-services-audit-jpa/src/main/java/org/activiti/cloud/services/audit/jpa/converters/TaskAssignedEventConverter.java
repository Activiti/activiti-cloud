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
        CloudTaskAssignedEvent cloudTaskAssignedEvent = (CloudTaskAssignedEvent) cloudRuntimeEvent;
                
        return new TaskAssignedEventEntity(cloudTaskAssignedEvent.getId(),
                                           cloudTaskAssignedEvent.getTimestamp(),
                                           cloudTaskAssignedEvent.getAppName(),
                                           cloudTaskAssignedEvent.getAppVersion(),
                                           cloudTaskAssignedEvent.getServiceName(),
                                           cloudTaskAssignedEvent.getServiceFullName(),
                                           cloudTaskAssignedEvent.getServiceType(),
                                           cloudTaskAssignedEvent.getServiceVersion(),
                                           cloudTaskAssignedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskAssignedEventEntity taskAssignedEventEntity = (TaskAssignedEventEntity) auditEventEntity;

        CloudTaskAssignedEventImpl cloudTaskAssignedEvent = new CloudTaskAssignedEventImpl(taskAssignedEventEntity.getEventId(),
                                                                                           taskAssignedEventEntity.getTimestamp(),
                                                                                           taskAssignedEventEntity.getTask());
        cloudTaskAssignedEvent.setAppName(taskAssignedEventEntity.getAppName());
        cloudTaskAssignedEvent.setAppVersion(taskAssignedEventEntity.getAppVersion());
        cloudTaskAssignedEvent.setServiceFullName(taskAssignedEventEntity.getServiceFullName());
        cloudTaskAssignedEvent.setServiceName(taskAssignedEventEntity.getServiceName());
        cloudTaskAssignedEvent.setServiceType(taskAssignedEventEntity.getServiceType());
        cloudTaskAssignedEvent.setServiceVersion(taskAssignedEventEntity.getServiceVersion());

        return cloudTaskAssignedEvent;
    }
}
