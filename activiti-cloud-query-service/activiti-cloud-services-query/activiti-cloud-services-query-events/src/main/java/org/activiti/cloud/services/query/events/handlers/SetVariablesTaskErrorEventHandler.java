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
import java.util.Date;
import java.util.Optional;
import org.activiti.api.process.model.events.SetVariablesTaskErrorEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.SetVariablesTaskErrorEvent;
import org.activiti.cloud.services.query.model.BaseBPMNActivityEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SET_VARIABLES_TASK_ERROR_RECEIVED events from the runtime bundle.
 * Marks the SetVariablesTask execution as failed with error information.
 */
public class SetVariablesTaskErrorEventHandler implements QueryEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(SetVariablesTaskErrorEventHandler.class);

    private final EntityManager entityManager;

    public SetVariablesTaskErrorEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        SetVariablesTaskErrorEvent errorEvent = (SetVariablesTaskErrorEvent) event;

        try {
            String executionId = errorEvent.getEntity().getExecutionId();
            String processInstanceId = errorEvent.getProcessInstanceId();

            Optional<BaseBPMNActivityEntity> optionalActivity = findActivityByExecutionId(
                processInstanceId,
                executionId
            );

            if (optionalActivity.isPresent()) {
                BaseBPMNActivityEntity activity = optionalActivity.get();

                activity.setStatus(CloudBPMNActivity.BPMNActivityStatus.ERROR);
                activity.setCompletedDate(new Date(errorEvent.getTimestamp()));

                if (activity.getStartedDate() == null) {
                    activity.setStartedDate(new Date(errorEvent.getTimestamp()));
                }

                entityManager.persist(activity);

                logger.debug(
                    "Marked SetVariablesTask execution {} as failed: {}",
                    executionId,
                    errorEvent.getEntity().getErrorMessage()
                );
            } else {
                logger.warn(
                    "Could not find activity for SetVariablesTask execution {} in process {}",
                    executionId,
                    processInstanceId
                );
            }
        } catch (Exception e) {
            logger.error("Error handling SetVariablesTaskErrorEvent", e);
            throw new QueryException("Error processing SetVariablesTaskErrorEvent", e);
        }
    }

    private Optional<BaseBPMNActivityEntity> findActivityByExecutionId(String processInstanceId, String executionId) {
        try {
            BaseBPMNActivityEntity activity = entityManager
                .createQuery(
                    "SELECT a FROM BaseBPMNActivityEntity a " +
                        "WHERE a.processInstanceId = :processInstanceId " +
                        "AND a.executionId = :executionId " +
                        "ORDER BY a.completedDate DESC, a.id DESC",
                    BaseBPMNActivityEntity.class
                )
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("executionId", executionId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

            return Optional.ofNullable(activity);
        } catch (Exception e) {
            logger.warn("Error finding activity for execution {}: {}", executionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getHandledEvent() {
        return SetVariablesTaskErrorEvent.SetVariablesTaskErrorEvents.SET_VARIABLES_TASK_ERROR_RECEIVED.name();
    }
}
