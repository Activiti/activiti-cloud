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
package org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.AuditPayloadCleanup;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.AuditPayloadUpdateStrategy;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariablePayloadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientAuditPayloadCleanup implements AuditPayloadCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransientAuditPayloadCleanup.class);

    private final EntityManager entityManager;

    private final List<AuditPayloadUpdateStrategy> payloadUpdateStrategies;

    public TransientAuditPayloadCleanup(
        EntityManager entityManager,
        List<AuditPayloadUpdateStrategy> payloadUpdateStrategies
    ) {
        this.entityManager = entityManager;
        this.payloadUpdateStrategies = List.copyOf(payloadUpdateStrategies);
    }

    @Override
    public void cleanup(List<AuditEventEntity> receivedEvents) {
        entityManager.flush();
        entityManager.clear();
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
                      IntegrationErrorReceivedEvent,
                      MessageWaitingEvent,
                      MessageSentEvent,
                      MessageReceivedEvent,
                      SignalReceivedEvent
                  )
                """,
                AuditEventEntity.class
            )
            .setParameter("appName", appName)
            .setParameter("processInstanceId", processInstanceId)
            .getResultList();
        var eventsUpdated = new LinkedHashMap<String, Integer>();
        for (AuditEventEntity event : events) {
            if (event instanceof VariablePayloadEvent payloadEvent) {
                if (payloadEvent.isVariablePayloadEmpty()) {
                    continue;
                }
                var strategy = payloadUpdateStrategies
                    .stream()
                    .filter(candidate -> candidate.supports(event))
                    .findFirst()
                    .orElseThrow(() ->
                        new IllegalStateException("No payload update strategy for " + event.getClass().getName())
                    );
                payloadEvent.clearVariablePayloads();
                int updated = strategy.update(event);
                eventsUpdated.merge(strategy.getClass().getSimpleName(), updated, Integer::sum);
            }
        }
        LOGGER.info(
            "Executed Audit payload cleanup for processInstanceId={} appName={}: eventsUpdated={}",
            processInstanceId,
            appName,
            eventsUpdated
        );
    }
}
