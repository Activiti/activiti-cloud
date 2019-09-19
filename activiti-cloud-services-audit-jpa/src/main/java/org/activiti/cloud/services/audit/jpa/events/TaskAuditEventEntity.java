package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskJpaJsonConverter;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TaskAuditEventEntity extends AuditEventEntity {

    @Convert(converter = TaskJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private Task task;

    private String taskId;

    private String taskName;

    public TaskAuditEventEntity() {
    }

    public TaskAuditEventEntity(CloudTaskRuntimeEvent cloudEvent) {
        super(cloudEvent);
        setTask(cloudEvent.getEntity());
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
        if (task != null) {
            this.taskId = task.getId();
            this.taskName = task.getName();
        }
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(task, taskId, taskName);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TaskAuditEventEntity other = (TaskAuditEventEntity) obj;
        return Objects.equals(task, other.task) 
                && Objects.equals(taskId, other.taskId) 
                && Objects.equals(taskName, other.taskName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TaskAuditEventEntity [task=")
               .append(task)
               .append(", taskId=")
               .append(taskId)
               .append(", taskName=")
               .append(taskName)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }
}
