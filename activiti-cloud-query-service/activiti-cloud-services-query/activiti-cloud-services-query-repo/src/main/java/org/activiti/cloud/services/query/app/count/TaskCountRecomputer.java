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
package org.activiti.cloud.services.query.app.count;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Turns "these tasks changed" into "these audiences now have these counts".
 * <p>
 * This is the read side of pushing counts, and it is deliberately kept in the persistence tier so that
 * both deployments of the event consumer - bundled with the REST tier, or standalone - can use it.
 * <p>
 * It runs in its own transaction because it is called <em>after</em> the event batch has committed: the
 * counts have to see the committed state, and the batch's own transaction is gone by then.
 */
public class TaskCountRecomputer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCountRecomputer.class);

    private final TaskRepository taskRepository;
    private final TaskCandidateGroupRepository taskCandidateGroupRepository;
    private final TaskCandidateUserRepository taskCandidateUserRepository;
    private final SubscriberScopeRegistry subscriberScopeRegistry;
    private final int fanOutWarnThreshold;

    public TaskCountRecomputer(
        TaskRepository taskRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository,
        TaskCandidateUserRepository taskCandidateUserRepository,
        SubscriberScopeRegistry subscriberScopeRegistry,
        int fanOutWarnThreshold
    ) {
        this.taskRepository = taskRepository;
        this.taskCandidateGroupRepository = taskCandidateGroupRepository;
        this.taskCandidateUserRepository = taskCandidateUserRepository;
        this.subscriberScopeRegistry = subscriberScopeRegistry;
        this.fanOutWarnThreshold = fanOutWarnThreshold;
    }

    /**
     * Recomputes the {@link PushedTaskCountFilter#QUEUED} count for every audience the given tasks could
     * have changed it for. One COUNT query per audience; none at all when nothing is listening.
     *
     * @param taskIds            ids of the tasks touched by the committed batch
     * @param groupsNamedInBatch groups the batch's events named directly. Candidate-group events carry
     *                           their group id, which is the only way to learn about a <em>removed</em>
     *                           candidate group: its row no longer exists to be read back.
     * @return one change per affected audience, empty when there is nothing to push
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<TaskCountChange> recompute(Set<String> taskIds, Set<String> groupsNamedInBatch) {
        if (CollectionUtils.isEmpty(taskIds) && CollectionUtils.isEmpty(groupsNamedInBatch)) {
            return List.of();
        }
        if (subscriberScopeRegistry.size() == 0) {
            // Nobody has fetched a count recently, so there is no audience to push to.
            return List.of();
        }

        Set<List<String>> scopes = scopesToRecompute(taskIds, groupsNamedInBatch);
        if (scopes.isEmpty()) {
            return List.of();
        }
        if (scopes.size() > fanOutWarnThreshold) {
            LOGGER.warn(
                "Recomputing task counts for {} group sets from one event batch, above the {} threshold: " +
                    "every group set costs one COUNT query. Raise query.count-scopes.fan-out-warn-threshold if " +
                    "this is expected for this deployment.",
                scopes.size(),
                fanOutWarnThreshold
            );
        }

        return scopes
            .stream()
            .map(groups ->
                TaskCountChange.forGroups(
                    groups,
                    taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, groups))
                )
            )
            .toList();
    }

    private Set<List<String>> scopesToRecompute(Set<String> taskIds, Set<String> groupsNamedInBatch) {
        Set<TaskCandidateGroupEntity> candidateGroupRows = taskIds.isEmpty()
            ? Set.of()
            : taskCandidateGroupRepository.findByTaskIdIn(taskIds);

        if (anyTaskVisibleToEveryAudience(taskIds, candidateGroupRows)) {
            // A task with no candidates at all is visible through the specification's "no candidate users
            // or groups" branch, which every audience matches, so there is nothing to narrow by.
            LOGGER.debug("A task in the batch has no candidates; recomputing every recorded group set");
            return subscriberScopeRegistry.allGroupSets();
        }

        Set<String> affectedGroups = candidateGroupRows
            .stream()
            .map(TaskCandidateGroupEntity::getGroupId)
            .collect(Collectors.toCollection(HashSet::new));
        if (!CollectionUtils.isEmpty(groupsNamedInBatch)) {
            affectedGroups.addAll(groupsNamedInBatch);
        }
        return subscriberScopeRegistry.groupSetsIntersecting(affectedGroups);
    }

    private boolean anyTaskVisibleToEveryAudience(
        Set<String> taskIds,
        Set<TaskCandidateGroupEntity> candidateGroupRows
    ) {
        Set<String> withoutCandidateGroups = new HashSet<>(taskIds);
        candidateGroupRows.forEach(row -> withoutCandidateGroups.remove(row.getTaskId()));
        if (withoutCandidateGroups.isEmpty()) {
            return false;
        }

        Set<String> withCandidateUsers = taskCandidateUserRepository
            .findByTaskIdIn(withoutCandidateGroups)
            .stream()
            .map(TaskCandidateUserEntity::getTaskId)
            .collect(Collectors.toSet());
        return !withCandidateUsers.containsAll(withoutCandidateGroups);
    }
}
