/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.events;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.events.TaskRuntimeEvent;

@Entity
@DiscriminatorValue(value = TaskCancelledEventEntity.TASK_CANCELLED_EVENT)
public class TaskCancelledEventEntity extends TaskAuditEventEntity {

    protected static final String TASK_CANCELLED_EVENT = "TaskCancelledEvent";

    public TaskCancelledEventEntity() {
    }

    public TaskCancelledEventEntity(String eventId,
                                    Long timestamp,
                                    String cause) {
        super(eventId,
              timestamp,
              TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name());
        this.cause = cause;
    }

    public TaskCancelledEventEntity(String eventId,
                                    Long timestamp,
                                    String appName,
                                    String appVersion,
                                    String serviceName,
                                    String serviceFullName,
                                    String serviceType,
                                    String serviceVersion,
                                    Task task,
                                    String cause) {
        super(eventId,
              timestamp,
              TaskRuntimeEvent.TaskEvents.TASK_CANCELLED.name(),
              appName,
              appVersion,
              serviceName,
              serviceFullName,
              serviceType,
              serviceVersion,
              task);
        this.cause = cause;
    }

    private String cause;

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
