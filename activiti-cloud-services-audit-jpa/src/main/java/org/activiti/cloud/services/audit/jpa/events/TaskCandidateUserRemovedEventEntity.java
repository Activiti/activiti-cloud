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

import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.api.task.model.events.TaskCandidateUserEvent;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskCandidateUserJpaJsonConverter;

@Entity
@DiscriminatorValue(value = TaskCandidateUserRemovedEventEntity.TASK_CANDIDATE_USER_REMOVED_EVENT)
public class TaskCandidateUserRemovedEventEntity extends AuditEventEntity {

    protected static final String TASK_CANDIDATE_USER_REMOVED_EVENT = "TaskCandidateUserRemovedEvent";

    @Convert(converter = TaskCandidateUserJpaJsonConverter.class)
    @Lob
    @Column
    private TaskCandidateUserImpl candidateUser;
    
    public TaskCandidateUserRemovedEventEntity() {
    }

    public TaskCandidateUserRemovedEventEntity(String eventId,
                                               Long timestamp,
                                               TaskCandidateUser candidateUser) {
        super(eventId,
              timestamp,
              TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED.name());
        
        setCandidateUser(candidateUser);
    }
    
    public TaskCandidateUser getCandidateUser() {
        return candidateUser;
    }
    
    public void setCandidateUser(TaskCandidateUser candidateUser) {
        this.candidateUser = new TaskCandidateUserImpl(candidateUser.getUserId(),candidateUser.getTaskId());
    }
}
