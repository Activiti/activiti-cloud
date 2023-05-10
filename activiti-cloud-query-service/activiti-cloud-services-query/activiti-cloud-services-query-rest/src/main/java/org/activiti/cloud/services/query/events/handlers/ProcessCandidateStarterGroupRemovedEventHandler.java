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
package org.activiti.cloud.services.query.events.handlers;

import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.activiti.api.process.model.events.ProcessCandidateStarterGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupRemovedEvent;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterGroupEntity;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterGroupId;
import org.activiti.cloud.services.query.model.QueryException;

public class ProcessCandidateStarterGroupRemovedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public ProcessCandidateStarterGroupRemovedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCandidateStarterGroupRemovedEvent processCandidateStarterGroupRemovedEvent = (CloudProcessCandidateStarterGroupRemovedEvent) event;
        org.activiti.api.process.model.ProcessCandidateStarterGroup processCandidateStarterGroup = processCandidateStarterGroupRemovedEvent.getEntity();

        ProcessCandidateStarterGroupId id = new ProcessCandidateStarterGroupId(
            processCandidateStarterGroup.getProcessDefinitionId(),
            processCandidateStarterGroup.getGroupId()
        );
        try {
            Optional
                .ofNullable(entityManager.find(ProcessCandidateStarterGroupEntity.class, id))
                .ifPresent(entityManager::remove);
        } catch (Exception cause) {
            throw new QueryException("Error handling ProcessCandidateStarterGroupRemovedEvent[" + event + "]", cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessCandidateStarterGroupEvent.ProcessCandidateStarterGroupEvents.PROCESS_CANDIDATE_STARTER_GROUP_REMOVED.name();
    }
}
