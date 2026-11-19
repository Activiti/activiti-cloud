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
package org.activiti.cloud.services.audit.jpa.streams;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import org.activiti.cloud.services.audit.jpa.converters.json.IntegrationContextJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.IntegrationEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableAuditEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientAuditPayloadCleanup implements AuditPayloadCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransientAuditPayloadCleanup.class);

    private final EntityManager entityManager;

    private final IntegrationContextJpaJsonConverter integrationContextConverter =
        new IntegrationContextJpaJsonConverter();

    public TransientAuditPayloadCleanup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void cleanup(List<AuditEventEntity> receivedEvents) {
        entityManager.flush();
        var cleanedProcesses = new HashSet<String>();
        for (AuditEventEntity event : receivedEvents) {
            if (event instanceof ProcessCompletedEventEntity || event instanceof ProcessCancelledAuditEventEntity) {
                String processInstanceId = event.getProcessInstanceId();
                if (processInstanceId != null && cleanedProcesses.add(processInstanceId)) {
                    cleanProcess(processInstanceId, event.getAppName());
                }
            }
        }
    }

    private void cleanProcess(String processInstanceId, String appName) {
        LOGGER.debug("Starting Audit payload cleanup for processInstanceId={} appName={}", processInstanceId, appName);
        var events = entityManager
            .createQuery(
                """
                select e from AuditEvent e
                where e.appName = :appName
                  and e.processInstanceId = :processInstanceId
                  and type(e) in (
                      VariableCreatedEvent,
                      VariableUpdatedEvent,
                      VariableDeletedEvent,
                      IntegrationRequestSentEvent,
                      IntegrationResultReceivedEvent,
                      IntegrationErrorReceivedEvent
                  )
                """,
                AuditEventEntity.class
            )
            .setParameter("appName", appName)
            .setParameter("processInstanceId", processInstanceId)
            .getResultList();
        int variableEventsUpdated = 0;
        int integrationEventsUpdated = 0;
        for (AuditEventEntity event : events) {
            if (event instanceof VariableAuditEventEntity variableEvent) {
                variableEventsUpdated += clearVariablePayload(variableEvent);
            } else if (event instanceof IntegrationEventEntity integrationEvent) {
                integrationEventsUpdated += clearIntegrationPayload(integrationEvent);
            }
        }
        LOGGER.info(
            "Executed Audit payload cleanup for processInstanceId={} appName={}: " +
                "variableEventsUpdated={}, integrationEventsUpdated={}",
            processInstanceId,
            appName,
            variableEventsUpdated,
            integrationEventsUpdated
        );
    }

    private int clearVariablePayload(VariableAuditEventEntity event) {
        event.clearVariablePayloads();

        return entityManager
            .createNativeQuery(
                """
                update audit_event
                set variable_instance = null,
                    variable_previous_value = null
                where id = :id
                """
            )
            .setParameter("id", event.getId())
            .executeUpdate();
    }

    private int clearIntegrationPayload(IntegrationEventEntity event) {
        event.clearVariablePayloads();

        String integrationContext = integrationContextConverter.convertToDatabaseColumn(event.getIntegrationContext());

        return entityManager
            .createNativeQuery(
                """
                update audit_event
                set integration_context = :integrationContext
                where id = :id
                """
            )
            .setParameter("integrationContext", integrationContext)
            .setParameter("id", event.getId())
            .executeUpdate();
    }
}
