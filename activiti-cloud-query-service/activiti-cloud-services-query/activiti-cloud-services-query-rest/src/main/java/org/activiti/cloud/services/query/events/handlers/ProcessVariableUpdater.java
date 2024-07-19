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

import jakarta.persistence.EntityManager;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableInstance;
import org.activiti.cloud.services.query.model.ProcessVariablesPivotEntity;
import org.activiti.cloud.services.query.model.QueryException;

public class ProcessVariableUpdater {

    private final EntityManager entityManager;
    private final EntityManagerFinder entityManagerFinder;

    public ProcessVariableUpdater(EntityManager entityManager, EntityManagerFinder entityManagerFinder) {
        this.entityManager = entityManager;
        this.entityManagerFinder = entityManagerFinder;
    }

    public void update(ProcessVariableInstance updatedVariableEntity, String notFoundMessage) {
        String processInstanceId = updatedVariableEntity.getProcessInstanceId();
        ProcessInstanceEntity processInstanceEntity = entityManagerFinder
            .findProcessInstanceWithVariables(processInstanceId)
            .orElseThrow(() -> new QueryException("Process instance id " + processInstanceId + " not found!"));
        ProcessVariablesPivotEntity processVariablesPivot = processInstanceEntity.getProcessVariablesPivot();
        processVariablesPivot
            .getValues()
            .compute(
                updatedVariableEntity.getName(),
                (k, v) -> {
                    if (v == null) {
                        throw new QueryException(notFoundMessage);
                    }
                    return updatedVariableEntity;
                }
            );
        entityManager.persist(processVariablesPivot);
    }
}
