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
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserRemovedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.TaskCandidateUserJpaJsonConverter;

@Entity(name = TaskCandidateUserRemovedEventEntity.TASK_CANDIDATE_USER_REMOVED_EVENT)
@DiscriminatorValue(value = TaskCandidateUserRemovedEventEntity.TASK_CANDIDATE_USER_REMOVED_EVENT)
public class TaskCandidateUserRemovedEventEntity extends AuditEventEntity {

    protected static final String TASK_CANDIDATE_USER_REMOVED_EVENT = "TaskCandidateUserRemovedEvent";

    @Convert(converter = TaskCandidateUserJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private TaskCandidateUserImpl candidateUser;

    public TaskCandidateUserRemovedEventEntity() {}

    public TaskCandidateUserRemovedEventEntity(CloudTaskCandidateUserRemovedEvent cloudEvent) {
        super(cloudEvent);
        setCandidateUser(cloudEvent.getEntity());
    }

    public TaskCandidateUser getCandidateUser() {
        return candidateUser;
    }

    public void setCandidateUser(TaskCandidateUser candidateUser) {
        this.candidateUser = new TaskCandidateUserImpl(candidateUser.getUserId(), candidateUser.getTaskId());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("TaskCandidateUserRemovedEventEntity [candidateUser=")
            .append(candidateUser)
            .append(", toString()=")
            .append(super.toString())
            .append("]");
        return builder.toString();
    }
}
