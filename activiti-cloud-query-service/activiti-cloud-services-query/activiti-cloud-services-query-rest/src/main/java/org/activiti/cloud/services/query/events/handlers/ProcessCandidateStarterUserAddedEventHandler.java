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

import org.activiti.api.process.model.events.ProcessCandidateStarterUserEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserAddedEvent;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterUserEntity;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterUserId;
import org.activiti.cloud.services.query.model.QueryException;

import javax.persistence.EntityManager;

public class ProcessCandidateStarterUserAddedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public ProcessCandidateStarterUserAddedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCandidateStarterUserAddedEvent processCandidateStarterUserAddedEvent = (CloudProcessCandidateStarterUserAddedEvent) event;
        org.activiti.api.process.model.ProcessCandidateStarterUser processCandidateStarterUser = processCandidateStarterUserAddedEvent.getEntity();
        ProcessCandidateStarterUserEntity entity = new ProcessCandidateStarterUserEntity(processCandidateStarterUser.getProcessDefinitionId(),
                                                                                         processCandidateStarterUser.getUserId());

        try {
            if (!candidateStarterEntityAlreadyExists(entity)) {
                entityManager.persist(entity);
            }
        } catch (Exception cause) {
            throw new QueryException("Error handling ProcessCandidateStarterUserAddedEvent[" + event + "]",
                                     cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_ADDED.name();
    }

    private boolean candidateStarterEntityAlreadyExists(ProcessCandidateStarterUserEntity entity) {
        return entityManager.find(ProcessCandidateStarterUserEntity.class,
                                  new ProcessCandidateStarterUserId(entity.getProcessDefinitionId(), entity.getUserId())) != null;

    }
}
