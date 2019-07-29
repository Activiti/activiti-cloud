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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateGroupRemovedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskCandidateGroupJpaJsonConverter;

@Entity
@DiscriminatorValue(value = TaskCandidateGroupRemovedEventEntity.TASK_CANDIDATE_GROUP_REMOVED_EVENT)
public class TaskCandidateGroupRemovedEventEntity extends AuditEventEntity {

    protected static final String TASK_CANDIDATE_GROUP_REMOVED_EVENT = "TaskCandidateGroupRemovedEvent";

    @Convert(converter = TaskCandidateGroupJpaJsonConverter.class)
    @Lob
    @Column
    private TaskCandidateGroupImpl candidateGroup;
    
    public TaskCandidateGroupRemovedEventEntity() {
    }

    public TaskCandidateGroupRemovedEventEntity(CloudTaskCandidateGroupRemovedEvent cloudEvent) {
        super(cloudEvent);
        setCandidateGroup(cloudEvent.getEntity());
    }
    
    public TaskCandidateGroup getCandidateGroup() {
        return candidateGroup;
    }

    public void setCandidateGroup(TaskCandidateGroup candidateGroup) {
        this.candidateGroup = new TaskCandidateGroupImpl(candidateGroup.getGroupId(),candidateGroup.getTaskId());
    }
}
