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

package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.task.model.events.TaskRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskUpdatedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskUpdatedEventEntity;

public class TaskUpdatedEventConverter extends BaseEventToEntityConverter {

    public TaskUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskRuntimeEvent.TaskEvents.TASK_UPDATED.name();
    }

    @Override
    public TaskUpdatedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {
        CloudTaskUpdatedEvent cloudTaskUpdatedEvent = (CloudTaskUpdatedEvent) cloudRuntimeEvent;
                
        return new TaskUpdatedEventEntity(cloudTaskUpdatedEvent.getId(),
                                          cloudTaskUpdatedEvent.getTimestamp(),
                                          cloudTaskUpdatedEvent.getAppName(),
                                          cloudTaskUpdatedEvent.getAppVersion(),
                                          cloudTaskUpdatedEvent.getServiceName(),
                                          cloudTaskUpdatedEvent.getServiceFullName(),
                                          cloudTaskUpdatedEvent.getServiceType(),
                                          cloudTaskUpdatedEvent.getServiceVersion(),
                                          cloudTaskUpdatedEvent.getEntity());
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskUpdatedEventEntity taskUpdatedEventEntity = (TaskUpdatedEventEntity) auditEventEntity;

        CloudTaskUpdatedEventImpl cloudTaskUpdatedEvent = new CloudTaskUpdatedEventImpl(taskUpdatedEventEntity.getEventId(),
                                                                                         taskUpdatedEventEntity.getTimestamp(),
                                                                                         taskUpdatedEventEntity.getTask());
        cloudTaskUpdatedEvent.setAppName(taskUpdatedEventEntity.getAppName());
        cloudTaskUpdatedEvent.setAppVersion(taskUpdatedEventEntity.getAppVersion());
        cloudTaskUpdatedEvent.setServiceFullName(taskUpdatedEventEntity.getServiceFullName());
        cloudTaskUpdatedEvent.setServiceName(taskUpdatedEventEntity.getServiceName());
        cloudTaskUpdatedEvent.setServiceType(taskUpdatedEventEntity.getServiceType());
        cloudTaskUpdatedEvent.setServiceVersion(taskUpdatedEventEntity.getServiceVersion());

        return cloudTaskUpdatedEvent;
    }
}
