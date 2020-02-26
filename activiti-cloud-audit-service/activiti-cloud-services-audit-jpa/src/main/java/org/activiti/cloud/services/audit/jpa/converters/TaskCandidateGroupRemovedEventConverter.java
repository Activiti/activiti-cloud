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

import org.activiti.api.task.model.events.TaskCandidateGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupRemovedEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TaskCandidateGroupRemovedEventEntity;

public class TaskCandidateGroupRemovedEventConverter extends BaseEventToEntityConverter {

    public TaskCandidateGroupRemovedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        super(eventContextInfoAppender);
    }
    
    @Override
    public String getSupportedEvent() {
        return TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED.name();
    }

    @Override
    public TaskCandidateGroupRemovedEventEntity createEventEntity(CloudRuntimeEvent cloudRuntimeEvent) {             
        return new TaskCandidateGroupRemovedEventEntity((CloudTaskCandidateGroupRemovedEvent) cloudRuntimeEvent);
    }

    @Override
    protected CloudRuntimeEventImpl<?, ?> createAPIEvent(AuditEventEntity auditEventEntity) {
        TaskCandidateGroupRemovedEventEntity eventEntity = (TaskCandidateGroupRemovedEventEntity) auditEventEntity;

        return new CloudTaskCandidateGroupRemovedEventImpl(eventEntity.getEventId(),
                                                           eventEntity.getTimestamp(),
                                                           eventEntity.getCandidateGroup());
    }
}
