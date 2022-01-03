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
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessUpdatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.api.task.model.events.CloudTaskRuntimeEvent;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskActivatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskAssignedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCancelledEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCompletedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskSuspendedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskUpdatedEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QueryEventHandlerContextOptimizer {
    public static final String VARIABLES = "variables";
    public static final String TASKS = "tasks";
    public static final String ACTIVITIES = "activities";
    private static Logger LOGGER = LoggerFactory.getLogger(QueryEventHandlerContextOptimizer.class);

    private Map<Class<? extends CloudRuntimeEvent>, Integer> order =
         Map.ofEntries(Map.entry(CloudRuntimeEvent.class, 0),
                       Map.entry(CloudProcessCreatedEventImpl.class, 0),
                       Map.entry(CloudProcessStartedEventImpl.class, 1),
                       Map.entry(CloudProcessUpdatedEventImpl.class, 1),
                       Map.entry(CloudSequenceFlowTakenEventImpl.class, 1),
                       Map.entry(CloudBPMNActivityStartedEventImpl.class, 1),
                       Map.entry(CloudBPMNActivityCompletedEventImpl.class, 1),
                       Map.entry(CloudBPMNActivityCancelledEventImpl.class, 1),
                       Map.entry(CloudIntegrationRequestedEventImpl.class, 1),
                       Map.entry(CloudIntegrationResultReceivedEventImpl.class, 1),
                       Map.entry(CloudIntegrationErrorReceivedEventImpl.class, 1),
                       Map.entry(CloudTaskCreatedEventImpl.class, 2),
                       Map.entry(CloudTaskCandidateUserAddedEventImpl.class, 3),
                       Map.entry(CloudTaskCandidateGroupAddedEventImpl.class, 3),
                       Map.entry(CloudVariableCreatedEventImpl.class, 3),
                       Map.entry(CloudVariableUpdatedEventImpl.class, 4),
                       Map.entry(CloudVariableDeletedEventImpl.class, 5),
                       Map.entry(CloudTaskActivatedEventImpl.class, 6),
                       Map.entry(CloudTaskSuspendedEventImpl.class, 6),
                       Map.entry(CloudTaskAssignedEventImpl.class, 6),
                       Map.entry(CloudTaskUpdatedEventImpl.class, 6),
                       Map.entry(CloudTaskCompletedEventImpl.class, 6),
                       Map.entry(CloudTaskCancelledEventImpl.class, 6),
                       Map.entry(CloudTaskCandidateUserRemovedEventImpl.class, 7),
                       Map.entry(CloudTaskCandidateGroupRemovedEventImpl.class, 7),
                       Map.entry(CloudProcessCompletedEventImpl.class, 8),
                       Map.entry(CloudProcessCancelledEventImpl.class, 8));

    private final EntityManager entityManager;

    public QueryEventHandlerContextOptimizer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<CloudRuntimeEvent<?,?>> optimize(List<CloudRuntimeEvent<?, ?>> events) {
        resolveProcessInstanceId(events)
            .ifPresent(processInstanceId -> {
                LOGGER.debug("Building entity fetch graph for root process instance: {}",
                             processInstanceId);
                EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

                findRuntimeEvent(events,
                                 CloudVariableEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes(VARIABLES));
                findRuntimeEvent(events,
                                 CloudTaskRuntimeEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes(TASKS));
                findRuntimeEvent(events,
                                 CloudBPMNActivityEvent.class)
                    .ifPresent(e -> entityGraph.addAttributeNodes(ACTIVITIES));

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

        return events.stream()
                     .sorted(Comparator.comparing(event -> Optional.ofNullable(order.get(event.getClass()))
                                                                   .orElseGet(() -> order.get(CloudRuntimeEvent.class))))
                     .collect(Collectors.toList());
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
