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
import javax.persistence.EntityManager;
import org.activiti.api.process.model.events.ProcessCandidateStarterUserEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCandidateStarterUserRemovedEvent;
import org.activiti.cloud.services.query.model.*;

public class ProcessCandidateStarterUserRemovedEventHandler implements QueryEventHandler {

    private final EntityManager entityManager;

    public ProcessCandidateStarterUserRemovedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessCandidateStarterUserRemovedEvent processCandidateStarterUserRemovedEvent = (CloudProcessCandidateStarterUserRemovedEvent) event;
        org.activiti.api.process.model.ProcessCandidateStarterUser processCandidateStarterUser = processCandidateStarterUserRemovedEvent.getEntity();

        ProcessCandidateStarterUserId id = new ProcessCandidateStarterUserId(
            processCandidateStarterUser.getProcessDefinitionId(),
            processCandidateStarterUser.getUserId()
        );

        try {
            Optional
                .ofNullable(entityManager.find(ProcessCandidateStarterUserEntity.class, id))
                .ifPresent(entityManager::remove);
        } catch (Exception cause) {
            throw new QueryException("Error handling ProcessCandidateStarterUserRemovedEvent[" + event + "]", cause);
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessCandidateStarterUserEvent.ProcessCandidateStarterUserEvents.PROCESS_CANDIDATE_STARTER_USER_REMOVED.name();
    }
}
