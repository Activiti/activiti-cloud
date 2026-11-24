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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientQueryPayloadCleanup implements QueryPayloadCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransientQueryPayloadCleanup.class);
    private static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String DELETE_VARIABLE_LINK = """
    delete from task_process_variable
    where process_variable_id in (select id from process_variable where process_instance_id = :processInstanceId)
    """;
    public static final String DELETE_TASK_VARIABLE =
        "delete from TaskVariable v where v.processInstanceId = :processInstanceId";
    public static final String DELETE_PROCESS_VARIABLE =
        "delete from ProcessVariable v where v.processInstanceId = :processInstanceId";
    public static final String CLEAR_INTEGRATION_CONTEXT = """
    update integration_context
    set inbound_variables = '{}', out_bound_variables = '{}'
    where process_instance_id = :processInstanceId
    """;

    public static final String UPDATE_LAST_VARIABLE_CLEANUP = """
    update process_instance
    set last_variable_cleanup = :cleanupTime
    where id = :processInstanceId
    """;

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
        long startTime = System.currentTimeMillis();
        LOGGER.info(
            "Starting Query payload cleanup for processInstanceId={} startTime={}",
            processInstanceId,
            startTime
        );
        entityManager.flush();

        int deletedVariableLinks = getDeletedVariableLinks(processInstanceId);
        int deletedTaskVariables = getDeletedTaskVariables(processInstanceId);
        int deletedProcessVariables = getDeletedProcessVariables(processInstanceId);

        int clearedIntegrationContexts = clearIntegrationVariablePayloads(processInstanceId);

        entityManager
            .createNativeQuery(UPDATE_LAST_VARIABLE_CLEANUP)
            .setParameter("cleanupTime", OffsetDateTime.now(ZoneOffset.UTC))
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();

        entityManager.clear();
        long endTime = System.currentTimeMillis();
        LOGGER.info(
            "Flushed Query payload cleanup for processInstanceId={}: deletedVariableLinks={}, " +
                "deletedTaskVariables={}, deletedProcessVariables={}, clearedIntegrationContexts={}, " +
                "startTime={}, endTime={}, elapsedMs={}",
            processInstanceId,
            deletedVariableLinks,
            deletedTaskVariables,
            deletedProcessVariables,
            clearedIntegrationContexts,
            startTime,
            endTime,
            endTime - startTime
        );
    }

    private int getDeletedVariableLinks(String processInstanceId) {
        return entityManager
            .createNativeQuery(DELETE_VARIABLE_LINK)
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private int getDeletedTaskVariables(String processInstanceId) {
        return entityManager
            .createQuery(DELETE_TASK_VARIABLE)
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private int getDeletedProcessVariables(String processInstanceId) {
        return entityManager
            .createQuery(DELETE_PROCESS_VARIABLE)
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }

    private int clearIntegrationVariablePayloads(String processInstanceId) {
        return entityManager
            .createNativeQuery(CLEAR_INTEGRATION_CONTEXT)
            .setParameter(PROCESS_INSTANCE_ID, processInstanceId)
            .executeUpdate();
    }
}
