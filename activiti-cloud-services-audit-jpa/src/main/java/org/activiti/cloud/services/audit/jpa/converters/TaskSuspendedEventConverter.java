package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskSuspendedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskSuspendedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskSuspendedEventEntity;

public class TaskSuspendedEventConverter extends BaseEventToEntityConverter {

    public TaskSuspendedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED.name();
    }

    @Override
    protected TaskSuspendedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskSuspendedEvent cloudTaskSuspendedEvent = (CloudTaskSuspendedEvent) cloudRuntimeEvent;
        return new TaskSuspendedEventEntity(cloudTaskSuspendedEvent.getId(),
                                            cloudTaskSuspendedEvent.getTimestamp(),
                                            cloudTaskSuspendedEvent.getAppName(),
                                            cloudTaskSuspendedEvent.getAppVersion(),
                                            cloudTaskSuspendedEvent.getServiceName(),
                                            cloudTaskSuspendedEvent.getServiceFullName(),
                                            cloudTaskSuspendedEvent.getServiceType(),
                                            cloudTaskSuspendedEvent.getServiceVersion(),
                                            cloudTaskSuspendedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
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
