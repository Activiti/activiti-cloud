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

import org.activiti.cloud.api.task.model.events.CloudTaskSuspendedEvent;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity(name = TaskSuspendedEventEntity.TASK_SUSPENDED_EVENT)
@DiscriminatorValue(value = TaskSuspendedEventEntity.TASK_SUSPENDED_EVENT)
@DynamicInsert
public class TaskSuspendedEventEntity extends TaskAuditEventEntity {

    protected static final String TASK_SUSPENDED_EVENT = "TaskSuspendedEvent";

    public TaskSuspendedEventEntity() {}

    public TaskSuspendedEventEntity(CloudTaskSuspendedEvent cloudEvent) {
        super(cloudEvent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TaskSuspendedEventEntity [toString()=")
                .append(super.toString())
                .append("]");
        return builder.toString();
    }
}
