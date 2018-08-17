package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskAssignedEventEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskAssignedEventConverter implements EventToEntityConverter<AuditEventEntity> {

    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED.name();
    }

    @Override
    public AuditEventEntity convertToEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskAssignedEvent cloudTaskAssignedEvent = (CloudTaskAssignedEvent) cloudRuntimeEvent;
        TaskAssignedEventEntity taskAssignedEventEntity = new TaskAssignedEventEntity(cloudTaskAssignedEvent.getId(),
                                                                                      cloudTaskAssignedEvent.getTimestamp(),
                                                                                      cloudTaskAssignedEvent.getAppName(),
                                                                                      cloudTaskAssignedEvent.getAppVersion(),
                                                                                      cloudTaskAssignedEvent.getServiceFullName(),
                                                                                      cloudTaskAssignedEvent.getServiceName(),
                                                                                      cloudTaskAssignedEvent.getServiceType(),
                                                                                      cloudTaskAssignedEvent.getServiceVersion(),
                                                                                      cloudTaskAssignedEvent.getEntity());
        return taskAssignedEventEntity;
    }

    @Override
    public CloudRuntimeEvent convertToAPI(AuditEventEntity auditEventEntity) {
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
