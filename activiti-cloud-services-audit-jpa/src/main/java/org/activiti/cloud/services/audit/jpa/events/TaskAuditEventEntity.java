package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.activiti.cloud.services.audit.jpa.converters.json.TaskJpaJsonConverter;
import org.activiti.api.task.model.Task;

@Entity
public abstract class TaskAuditEventEntity extends AuditEventEntity {

    @Convert(converter = TaskJpaJsonConverter.class)
    @Lob
    @Column
    private Task task;

    private String taskId;

    private String taskName;

    public TaskAuditEventEntity() {
    }

    public TaskAuditEventEntity(String eventId,
                                Long timestamp,
                                String eventType) {
        super(eventId,
              timestamp,
              eventType);
    }

    public TaskAuditEventEntity(String eventId,
                                Long timestamp,
                                String eventType,
                                String appName,
                                String appVersion,
                                String serviceName,
                                String serviceFullName,
                                String serviceType,
                                String serviceVersion,
                                Task task) {
        super(eventId,
              timestamp,
              eventType);
        setAppName(appName);
        setAppVersion(appVersion);
        setServiceName(serviceName);
        setServiceFullName(serviceFullName);
        setServiceType(serviceType);
        setServiceVersion(serviceVersion);
        setProcessDefinitionId((task != null) ? task.getProcessDefinitionId() : null);
        setProcessInstanceId((task != null) ? task.getProcessInstanceId() : null);

        this.task = task;
        if (task != null) {
            this.taskId = task.getId();
            this.taskName = task.getName();
            setEntityId(task.getId());
        }

    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}
