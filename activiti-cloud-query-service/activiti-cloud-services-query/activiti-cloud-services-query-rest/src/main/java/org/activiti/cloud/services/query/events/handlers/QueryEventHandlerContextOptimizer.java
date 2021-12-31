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

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QueryEventHandlerContextOptimizer {
    private static Logger LOGGER = LoggerFactory.getLogger(QueryEventHandlerContextOptimizer.class);

    private final EntityManager entityManager;

    public QueryEventHandlerContextOptimizer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void optimize(List<CloudRuntimeEvent<?, ?>> events) {
        resolveProcessInstanceId(events)
            .ifPresent(processInstanceId -> {
                LOGGER.debug("Building entity fetch graph for root process instance: {}",
                             processInstanceId);
                EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

                findRuntimeEvent(events,
                                 CloudVariableEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes("variables"));
                findRuntimeEvent(events,
                                 CloudTaskRuntimeEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes("tasks"));
                findRuntimeEvent(events,
                                 CloudBPMNActivityEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes("activities"));

                Optional.ofNullable(entityManager.find(ProcessInstanceEntity.class,
                                                       processInstanceId,
                                                       Map.of(QueryHints.HINT_LOADGRAPH,
                                                              entityGraph)))
                        .ifPresent(rootProcessInstance -> {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Fetched entity graph attributes {} for process instance: {}",
                                             entityGraph.getAttributeNodes().stream().map(AttributeNode::getAttributeName).collect(Collectors.toList()),
                                             processInstanceId);
                            }
                        });
            });

    }

    protected Optional<String> resolveProcessInstanceId(List<CloudRuntimeEvent<?, ?>> events) {
        if (events.stream()
                  .anyMatch(CloudProcessCreatedEvent.class::isInstance)) {
            return Optional.empty();
        }

        return events.stream()
                     .map(CloudRuntimeEvent::getProcessInstanceId)
                     .filter(Objects::nonNull)
                     .findFirst();
    }

    protected Optional<CloudRuntimeEvent<?, ?>> findRuntimeEvent(List<CloudRuntimeEvent<?, ?>> events,
                                                                 Class<? extends CloudRuntimeEvent<?, ?>> runtimeEventClass) {
        return events.stream()
                     .filter(runtimeEventClass::isInstance)
                     .findFirst();
    }

    protected <T> Optional<T> findRuntimeEvent(List<CloudRuntimeEvent<?, ?>> events,
                                               Class<? extends CloudRuntimeEvent<T, ?>> runtimeEventClass,
                                               Predicate<T> predicate) {
        return events.stream()
                     .filter(runtimeEventClass::isInstance)
                     .map(runtimeEventClass::cast)
                     .map(CloudRuntimeEvent::getEntity)
                     .filter(predicate)
                     .findFirst();
    }
}
