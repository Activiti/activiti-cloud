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
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
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

    private final int pageSize;

    public TransientAuditPayloadCleanup(
        EntityManager entityManager,
        List<AuditPayloadUpdateStrategy> payloadUpdateStrategies,
        int pageSize
    ) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        this.pageSize = pageSize;
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
        var eventsProcessed = new LinkedHashMap<String, Integer>();
        Long lastId = null;
        List<AuditEventEntity> events;
        do {
            events = this.findEvents(processInstanceId, appName, lastId);
            var eventsByStrategy = new LinkedHashMap<AuditPayloadUpdateStrategy, List<AuditEventEntity>>();
            for (AuditEventEntity event : events) {
                var payloadEvent = (VariablePayloadEvent) event;
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
                eventsByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(event);
            }
            eventsByStrategy.forEach((strategy, batch) -> {
                strategy.update(batch);
                eventsProcessed.merge(strategy.getClass().getSimpleName(), batch.size(), Integer::sum);
            });
            if (!events.isEmpty()) {
                lastId = events.getLast().getId();
            }
            entityManager.clear();
        } while (events.size() == pageSize);
        LOGGER.info(
            "Executed Audit payload cleanup for processInstanceId={} appName={}: eventsProcessed={}",
            processInstanceId,
            appName,
            eventsProcessed
        );
    }

    private List<AuditEventEntity> findEvents(String processInstanceId, String appName, Long lastId) {
        String query = """
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
        """;
        TypedQuery<AuditEventEntity> eventsQuery;
        if (lastId != null) {
            eventsQuery = entityManager
                .createQuery(query + " and e.id > :lastId order by e.id", AuditEventEntity.class)
                .setParameter("lastId", lastId);
        } else {
            eventsQuery = entityManager.createQuery(query + " order by e.id", AuditEventEntity.class);
        }
        return eventsQuery
            .setParameter("appName", appName)
            .setParameter("processInstanceId", processInstanceId)
            .setMaxResults(pageSize)
            .getResultList();
    }
}
