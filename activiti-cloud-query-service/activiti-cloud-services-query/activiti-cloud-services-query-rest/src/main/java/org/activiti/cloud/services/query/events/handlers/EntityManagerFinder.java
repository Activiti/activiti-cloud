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

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.hibernate.jpa.AvailableHints;

public class EntityManagerFinder {

    private static final String VARIABLES = "variables";
    private static final String TASKS = "tasks";
    private static final String ACTIVITIES = "activities";
    private static final String SERVICE_TASKS = "serviceTasks";
    private static final String SEQUENCE_FLOWS = "sequenceFlows";
    private static final String PROCESS_VARIABLES = "processVariables";
    private static final String TASK_CANDIDATE_USERS = "taskCandidateUsers";
    private static final String TASK_CANDIDATE_GROUPS = "taskCandidateGroups";
    private final EntityManager entityManager;

    public EntityManagerFinder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<TaskEntity> findTaskWithVariables(String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes(VARIABLES);

        return Optional.ofNullable(
            entityManager.find(TaskEntity.class, taskId, Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph))
        );
    }

    public List<TaskEntity> findTasksWithProcessVariables(String processInstanceId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);
        entityGraph.addAttributeNodes(PROCESS_VARIABLES);

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaskEntity> criteriaQuery = criteriaBuilder.createQuery(TaskEntity.class);
        Root<TaskEntity> root = criteriaQuery.from(TaskEntity.class);
        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("processInstanceId"), processInstanceId));
        return entityManager
            .createQuery(criteriaQuery)
            .setHint(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph)
            .getResultList();
    }

    public Optional<TaskEntity> findTaskWithCandidateUsers(String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes(TASK_CANDIDATE_USERS);

        return Optional.ofNullable(
            entityManager.find(TaskEntity.class, taskId, Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph))
        );
    }

    public Optional<TaskEntity> findTaskWithCandidateGroups(String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes(TASK_CANDIDATE_GROUPS);

        return Optional.ofNullable(
            entityManager.find(TaskEntity.class, taskId, Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph))
        );
    }

    public Optional<ProcessInstanceEntity> findProcessInstanceWithVariables(String processInstanceId) {
        EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

        entityGraph.addAttributeNodes(VARIABLES);

        return Optional.ofNullable(
            entityManager.find(
                ProcessInstanceEntity.class,
                processInstanceId,
                Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph)
            )
        );
    }

    public Optional<ProcessInstanceEntity> findProcessInstanceWithRelatedEntities(String processInstanceId) {
        EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

        entityGraph.addAttributeNodes(VARIABLES, TASKS, ACTIVITIES, SERVICE_TASKS, SEQUENCE_FLOWS);

        return Optional.ofNullable(
            entityManager.find(
                ProcessInstanceEntity.class,
                processInstanceId,
                Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph)
            )
        );
    }

    public Optional<ProcessInstanceEntity> findProcessInstanceWithSequenceFlows(String processInstanceId) {
        EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

        entityGraph.addAttributeNodes(SEQUENCE_FLOWS);

        return Optional.ofNullable(
            entityManager.find(
                ProcessInstanceEntity.class,
                processInstanceId,
                Map.of(AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph)
            )
        );
    }
}
