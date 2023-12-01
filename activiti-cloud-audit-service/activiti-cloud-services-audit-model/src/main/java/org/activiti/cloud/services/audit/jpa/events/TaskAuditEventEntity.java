/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.audit.jpa.events;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskJpaJsonConverter;

@MappedSuperclass
public abstract class TaskAuditEventEntity extends AuditEventEntity {

    @Convert(converter = TaskJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private Task task;

    private String taskId;

    private String taskName;

    public TaskAuditEventEntity() {}

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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("TaskAuditEventEntity [task=")
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
