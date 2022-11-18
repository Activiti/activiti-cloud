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

import javax.persistence.EntityManager;
import org.activiti.api.process.model.events.ProcessCandidateStarterGroupEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterGroupAddedEvent;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterGroupEntity;
import org.activiti.cloud.services.query.model.QueryException;

public class ProcessCandidateStarterGroupAddedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public ProcessCandidateStarterGroupAddedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCandidateStarterGroupAddedEvent processCandidateStarterGroupAddedEvent = (CloudProcessCandidateStarterGroupAddedEvent) event;
        org.activiti.api.process.model.ProcessCandidateStarterGroup processCandidateStarterGroup = processCandidateStarterGroupAddedEvent.getEntity();

        try {
            ProcessCandidateStarterGroupEntity entity = new ProcessCandidateStarterGroupEntity(
                processCandidateStarterGroup.getProcessDefinitionId(),
                processCandidateStarterGroup.getGroupId()
            );
            entityManager.persist(entity);
        } catch (Exception cause) {
            throw new QueryException("Error handling ProcessCandidateStarterGroupAddedEvent[" + event + "]", cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessCandidateStarterGroupEvent.ProcessCandidateStarterGroupEvents.PROCESS_CANDIDATE_STARTER_GROUP_ADDED.name();
    }
}
