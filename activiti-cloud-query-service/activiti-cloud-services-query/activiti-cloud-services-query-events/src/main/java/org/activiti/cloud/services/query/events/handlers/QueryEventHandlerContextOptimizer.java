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

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.events.CloudRuntimeEventSorter;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.hibernate.jpa.AvailableHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryEventHandlerContextOptimizer {

    public static final String VARIABLES = "variables";
    public static final String TASKS = "tasks";
    public static final String ACTIVITIES = "activities";
    public static final String SERVICE_TASKS = "serviceTasks";
    public static final String INTEGRATION_CONTEXTS = "integrationContexts";
    public static final String SEQUENCE_FLOWS = "sequenceFlows";
    private static Logger LOGGER = LoggerFactory.getLogger(QueryEventHandlerContextOptimizer.class);

    private final EntityManager entityManager;

    public QueryEventHandlerContextOptimizer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<CloudRuntimeEvent<?, ?>> optimize(List<CloudRuntimeEvent<?, ?>> events) {
        resolveProcessInstanceId(events).ifPresent(processInstanceId -> {
            LOGGER.debug("Building entity fetch graph for root process instance: {}", processInstanceId);
            var entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

            var criteriaBuilder = entityManager.getCriteriaBuilder();
            var criteriaQuery = criteriaBuilder.createQuery(ProcessInstanceEntity.class);
            var fromProcessInstance = criteriaQuery.from(ProcessInstanceEntity.class);
            var whereProcessInstance = criteriaBuilder.equal(fromProcessInstance.get("id"), processInstanceId);

            criteriaQuery.select(fromProcessInstance).where(whereProcessInstance);

            findRuntimeEvents(events, CloudVariableEvent.class, entity -> true, VariableInstance::getName).ifPresent(
                variableNames -> {
                    fetch(fromProcessInstance, entityGraph, VARIABLES, "name", variableNames);
                }
            );

            findRuntimeEvents(events, CloudTaskRuntimeEvent.class, entity -> true, Task::getId).ifPresent(taskIds -> {
                fetch(fromProcessInstance, entityGraph, TASKS, "id", taskIds);
            });

            findRuntimeEvents(
                events,
                CloudBPMNActivityEvent.class,
                entity -> true,
                BPMNActivityEntity.IdBuilderHelper::from
            ).ifPresent(activityIds -> {
                fetch(fromProcessInstance, entityGraph, ACTIVITIES, "id", activityIds);
            });

            findRuntimeEvents(
                events,
                CloudBPMNActivityEvent.class,
                entity -> SERVICE_TASKS.equals(entity.getActivityType()),
                BPMNActivityEntity.IdBuilderHelper::from
            ).ifPresent(serviceTaskIds -> {
                fetch(fromProcessInstance, entityGraph, SERVICE_TASKS, "id", serviceTaskIds);
            });

            findRuntimeEvents(events, CloudIntegrationEvent.class, entity -> true, IntegrationContext::getId).ifPresent(
                integrationContextIds -> {
                    fetch(fromProcessInstance, entityGraph, INTEGRATION_CONTEXTS, "id", integrationContextIds);
                }
            );

            entityManager
                .createQuery(criteriaQuery)
                .setHint(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph)
                .getResultList()
                .stream()
                .findFirst()
                .ifPresent(rootProcessInstance -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                            "Fetched entity graph attributes {} for process instance: {}",
                            entityGraph
                                .getAttributeNodes()
                                .stream()
                                .map(AttributeNode::getAttributeName)
                                .collect(Collectors.toList()),
                            processInstanceId
                        );
                    }
                });
        });

        return CloudRuntimeEventSorter.sort(events);
    }

    protected Optional<String> resolveProcessInstanceId(List<CloudRuntimeEvent<?, ?>> events) {
        return events.stream().map(CloudRuntimeEvent::getProcessInstanceId).filter(Objects::nonNull).findFirst();
    }

    protected Optional<CloudRuntimeEvent<?, ?>> findRuntimeEvent(
        List<CloudRuntimeEvent<?, ?>> events,
        Class<? extends CloudRuntimeEvent<?, ?>> runtimeEventClass
    ) {
        return events.stream().filter(runtimeEventClass::isInstance).findFirst();
    }

    protected <T, R> Optional<List<R>> findRuntimeEvents(
        List<CloudRuntimeEvent<?, ?>> events,
        Class<? extends CloudRuntimeEvent<T, ?>> runtimeEventClass,
        Predicate<T> predicate,
        Function<T, R> mapper
    ) {
        return Optional.of(
            events
                .stream()
                .filter(runtimeEventClass::isInstance)
                .map(runtimeEventClass::cast)
                .map(CloudRuntimeEvent::getEntity)
                .filter(predicate)
                .map(mapper)
                .distinct()
                .toList()
        ).filter(Predicate.not(List::isEmpty));
    }

    protected <T, R> void fetch(
        Root<T> from,
        EntityGraph<T> entityGraph,
        String association,
        String attribute,
        List<R> ids
    ) {
        entityGraph.addAttributeNodes(association);
        var join = (Join<?, ?>) from.fetch(association);
        join.on(join.get(attribute).in(ids));
    }
}
