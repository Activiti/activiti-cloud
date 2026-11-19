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
import java.util.List;
import java.util.Objects;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientQueryPayloadCleanup implements QueryPayloadCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransientQueryPayloadCleanup.class);
    private static final String PROCESS_INSTANCE_ID = "processInstanceId";

    private final EntityManager entityManager;

    public TransientQueryPayloadCleanup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void cleanup(List<CloudRuntimeEvent<?, ?>> events) {
        events
            .stream()
            .filter(event -> event instanceof CloudProcessCompletedEvent || event instanceof CloudProcessCancelledEvent)
            .map(CloudRuntimeEvent::getProcessInstanceId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(this::cleanProcess);
    }

    private void cleanProcess(String processInstanceId) {
        LOGGER.debug("Starting Query payload cleanup for processInstanceId={}", processInstanceId);
        entityManager.flush();

        int deletedVariableLinks = getDeletedVariableLinks(processInstanceId);
        int deletedTaskVariables = getDeletedTaskVariables(processInstanceId);
        int deletedProcessVariables = getDeletedProcessVariables(processInstanceId);

        var integrationContexts = getIntegrationContexts(processInstanceId);
        integrationContexts.forEach(IntegrationContextEntity::clearVariablePayloads);
        entityManager.flush();

        entityManager.clear();
        LOGGER.info(
            "Flushed Query payload cleanup for processInstanceId={}: deletedVariableLinks={}, " +
                "deletedTaskVariables={}, deletedProcessVariables={}, integrationContextsProcessed={}",
            processInstanceId,
            deletedVariableLinks,
            deletedTaskVariables,
            deletedProcessVariables,
            integrationContexts.size()
        );
    }

    private int getDeletedVariableLinks(String processInstanceId) {
        return entityManager
            .createNativeQuery(
                "delete from task_process_variable where process_variable_id in " +
                    "(select id from process_variable where process_instance_id = :processInstanceId)"
            )
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private int getDeletedTaskVariables(String processInstanceId) {
        return entityManager
            .createQuery("delete from TaskVariable v where v.processInstanceId = :processInstanceId")
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private int getDeletedProcessVariables(String processInstanceId) {
        return entityManager
            .createQuery("delete from ProcessVariable v where v.processInstanceId = :processInstanceId")
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private List<IntegrationContextEntity> getIntegrationContexts(String processInstanceId) {
        return entityManager
            .createQuery(
                "select c from IntegrationContext c where c.processInstanceId = :processInstanceId",
                IntegrationContextEntity.class
            )
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .getResultList();
    }
}
