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

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.hibernate.jpa.QueryHints;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import java.util.Map;
import java.util.Optional;

public class EntityManagerFinder {

    public Optional<TaskEntity> findTaskWithVariables(EntityManager entityManager,
                                                      String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes("variables");

        return Optional.ofNullable(entityManager.find(TaskEntity.class,
                                                      taskId,
                                                      Map.of(QueryHints.HINT_LOADGRAPH, entityGraph)));
    }

    public Optional<TaskEntity> findTaskWithCandidateUsers(EntityManager entityManager,
                                                           String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes("taskCandidateUsers");

        return Optional.ofNullable(entityManager.find(TaskEntity.class,
                                                      taskId,
                                                      Map.of(QueryHints.HINT_LOADGRAPH, entityGraph)));
    }

    public Optional<TaskEntity> findTaskWithCandidateGroups(EntityManager entityManager,
                                                            String taskId) {
        EntityGraph<TaskEntity> entityGraph = entityManager.createEntityGraph(TaskEntity.class);

        entityGraph.addAttributeNodes("taskCandidateGroups");

        return Optional.ofNullable(entityManager.find(TaskEntity.class,
                                                      taskId,
                                                      Map.of(QueryHints.HINT_LOADGRAPH, entityGraph)));
    }

    public Optional<ProcessInstanceEntity> findProcessInstanceWithVariables(EntityManager entityManager,
                                                                            String processInstanceId) {
        EntityGraph<ProcessInstanceEntity> entityGraph = entityManager.createEntityGraph(ProcessInstanceEntity.class);

        entityGraph.addAttributeNodes("variables");

        return Optional.ofNullable(entityManager.find(ProcessInstanceEntity.class,
                                                      processInstanceId,
                                                      Map.of(QueryHints.HINT_LOADGRAPH, entityGraph)));
    }
}
