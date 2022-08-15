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

import org.activiti.api.process.model.ProcessCandidateStarterUser;
import org.activiti.api.runtime.model.impl.ProcessCandidateStarterUserImpl;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserRemovedEvent;
import org.activiti.cloud.services.audit.jpa.converters.json.ProcessCandidateStarterUserJpaJsonConverter;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity(name = ProcessCandidateStarterUserRemovedEventEntity.PROCESS_CANDIDATE_STARTER_USER_REMOVED_EVENT)
@DiscriminatorValue(value = ProcessCandidateStarterUserRemovedEventEntity.PROCESS_CANDIDATE_STARTER_USER_REMOVED_EVENT)
@DynamicInsert
public class ProcessCandidateStarterUserRemovedEventEntity extends AuditEventEntity {

    protected static final String PROCESS_CANDIDATE_STARTER_USER_REMOVED_EVENT = "ProcessCandidateStarterUserRemovedEvent";

    @Convert(converter = ProcessCandidateStarterUserJpaJsonConverter.class)
    @Column(columnDefinition = "text")
    private ProcessCandidateStarterUserImpl candidateStarterUser;

    public ProcessCandidateStarterUserRemovedEventEntity() {
    }

    public ProcessCandidateStarterUserRemovedEventEntity(CloudProcessCandidateStarterUserRemovedEvent cloudEvent) {
        super(cloudEvent);
        setCandidateStarterUser(cloudEvent.getEntity());
    }

    public ProcessCandidateStarterUser getCandidateStarterUser() {
        return candidateStarterUser;
    }

    public void setCandidateStarterUser(ProcessCandidateStarterUser candidateUser) {
        this.candidateStarterUser = new ProcessCandidateStarterUserImpl(candidateUser.getProcessDefinitionId(), candidateUser.getUserId());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProcessCandidateStarterUserRemovedEventEntity [candidateStarterUser=")
               .append(candidateStarterUser)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }

}
