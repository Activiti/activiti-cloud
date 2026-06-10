/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import jakarta.persistence.EntityManager;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;

public class ProcessVariableUpdateEventHandler {

    private final ProcessVariableUpdater variableUpdater;
    private final EntityManager entityManager;

    public ProcessVariableUpdateEventHandler(ProcessVariableUpdater variableUpdater, EntityManager entityManager) {
        this.variableUpdater = variableUpdater;
        this.entityManager = entityManager;
    }

    public void handle(CloudVariableUpdatedEvent event) {
        ProcessVariableEntity variableEntity = new ProcessVariableEntity(event);
        variableEntity.setValue(event.getEntity().getValue());
        String variableName = variableEntity.getName();
        String processInstanceId = variableEntity.getProcessInstanceId();

        variableUpdater.update(
            variableEntity,
            "Unable to find variable named '" + variableName + "' for process instance '" + processInstanceId + "'"
        );

        if (!event.isEphemeralVariable()) {
            ProcessVariableHistoryEntity history = ProcessVariableHistoryEntityFactory.forUpdate(event);
            entityManager.persist(history);
        }
    }
}
