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
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessVariableDeletedEventHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(ProcessVariableDeletedEventHandler.class);

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public ProcessVariableDeletedEventHandler(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
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
                            // Persist into database
                            processInstanceEntity.getVariables().remove(variableEntity);

                            entityManager.remove(variableEntity);
                        },
                        () -> {
                            LOGGER.warn(
                                "Unable to find variableEntity with name '" +
                                variableName +
                                "' for process instance '" +
                                processInstanceId +
                                "'"
                            );
                        }
                    );
            } catch (Exception cause) {
                LOGGER.error("Error handling ProcessVariableDeletedEvent[" + event + "]", cause);
            }
        }
    }
}
