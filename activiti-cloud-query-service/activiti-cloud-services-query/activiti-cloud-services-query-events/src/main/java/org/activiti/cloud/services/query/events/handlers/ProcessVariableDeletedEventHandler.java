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
import java.util.Optional;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.common.feature.FeatureToggle;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessVariableDeletedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessVariableDeletedEventHandler.class);

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;
    private final FeatureToggle featureToggle;

    public ProcessVariableDeletedEventHandler(
        EntityManager entityManager,
        EntityManagerFinder entityManagerFinder,
        FeatureToggle featureToggle
    ) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
        this.featureToggle = featureToggle;
    }

    public void handle(CloudVariableDeletedEvent event) {
        String variableName = event.getEntity().getName();
        String processInstanceId = event.getEntity().getProcessInstanceId();
        Optional<ProcessInstanceEntity> findResult = entityManagerFinder.findProcessInstanceWithVariables(
            processInstanceId
        );
        // if a task was cancelled / completed do not handle this event
        if (findResult.isPresent() && !findResult.get().isInFinalState()) {
            try {
                ProcessInstanceEntity processInstanceEntity = findResult.get();

                processInstanceEntity
                    .getVariable(variableName)
                    .ifPresentOrElse(
                        variableEntity -> {
                            if (
                                !event.isEphemeralVariable() &&
                                featureToggle.isEnabled(QueryFeatureToggles.PROCESS_VARIABLE_HISTORY)
                            ) {
                                ProcessVariableHistoryEntity history = ProcessVariableHistoryEntityFactory.forDelete(
                                    event
                                );
                                entityManager.persist(history);
                            }

                            processInstanceEntity.getVariables().remove(variableEntity);
                            entityManager.remove(variableEntity);
                        },
                        () ->
                            LOGGER.warn(
                                "Unable to find variableEntity with name '{}' for process instance '{}'",
                                variableName,
                                processInstanceId
                            )
                    );
            } catch (Exception cause) {
                LOGGER.error("Error handling ProcessVariableDeletedEvent[{}]", event, cause);
            }
        }
    }
}
