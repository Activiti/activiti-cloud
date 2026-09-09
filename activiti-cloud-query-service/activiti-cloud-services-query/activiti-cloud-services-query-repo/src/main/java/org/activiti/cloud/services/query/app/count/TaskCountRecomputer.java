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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository.CandidateUserTaskCount;
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
 * Turns "these tasks changed" into "these users now see these many queued tasks".
 * <p>
 * The unit of output is a <b>user</b>, because that is the only unit a badge can be. A group-scoped count is
 * shareable and cheap but is not what the UI shows: it omits the assignee, owner and candidate-user branches,
 * and it cannot be repaired by adding a per-user count on the client, because visibility is a union rather
 * than a partition and the two overlap.
 * <p>
 * The unit of <em>work</em>, though, is still a group set. Per-user output must not mean per-user queries: a
 * task landing in a 500-member group would otherwise be 500 COUNTs for one event. Instead the affected users
 * are bucketed by the group set they hold, and each bucket costs two queries however many members it has:
 * <pre>
 *   queued(u) = shared(bucketGroups) + remainder(u, bucketGroups)
 * </pre>
 * The first term is one {@code forGroups} COUNT. The second is one {@code GROUP BY user_id} query whose
 * {@code NOT EXISTS} makes it a set difference, so the two terms are disjoint and adding them is legal.
 * <p>
 * It runs in its own transaction because it is called <em>after</em> the event batch has committed: the counts
 * have to see the committed state, and the batch's own transaction is gone by then.
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
     * Recomputes the {@link PushedTaskCountFilter#QUEUED} count for every subscriber the given tasks could
     * have changed it for. Two queries per distinct group set held by those subscribers; none at all when
     * nobody is watching.
     *
     * @param taskIds            ids of the tasks touched by the committed batch
     * @param groupsNamedInBatch groups the batch's events named directly. Candidate-group events carry their
     *                           group id, which is the only way to learn about a <em>removed</em> candidate
     *                           group: its row no longer exists to be read back.
     * @param usersNamedInBatch  users the batch's events named directly, for the same reason - a removed
     *                           candidate user is unrecoverable once the batch has committed
     * @return one change per affected subscriber, empty when there is nothing to push
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<TaskCountChange> recompute(
        Set<String> taskIds,
        Set<String> groupsNamedInBatch,
        Set<String> usersNamedInBatch
    ) {
        if (
            CollectionUtils.isEmpty(taskIds) &&
            CollectionUtils.isEmpty(groupsNamedInBatch) &&
            CollectionUtils.isEmpty(usersNamedInBatch)
        ) {
            return List.of();
        }
        if (subscriberScopeRegistry.size() == 0) {
            // Nobody is watching, so there is no count worth computing.
            return List.of();
        }

        Set<TaskCandidateGroupEntity> candidateGroupRows = taskIds.isEmpty()
            ? Set.of()
            : taskCandidateGroupRepository.findByTaskIdIn(taskIds);
        Set<TaskCandidateUserEntity> candidateUserRows = taskIds.isEmpty()
            ? Set.of()
            : taskCandidateUserRepository.findByTaskIdIn(taskIds);

        Set<String> affected = affectedSubscribers(
            taskIds,
            candidateGroupRows,
            candidateUserRows,
            groupsNamedInBatch,
            usersNamedInBatch
        );
        if (affected.isEmpty()) {
            return List.of();
        }

        // Two subscribers with the same group set share the expensive half of their count.
        Map<List<String>, List<String>> buckets = affected
            .stream()
            .collect(Collectors.groupingBy(subscriberScopeRegistry::groupsOf));
        warnOnFanOut(affected.size(), buckets.size());

        return buckets
            .entrySet()
            .stream()
            .flatMap(bucket -> queuedCountsFor(bucket.getKey(), bucket.getValue()).stream())
            .toList();
    }

    /**
     * Whose count may have moved. Two doors, and the second is the one the group-scoped design had no answer
     * for:
     * <ol>
     *   <li><b>the group door</b> - subscribers holding any of the groups the batch touched. Purely an
     *       in-memory scan of the registry.</li>
     *   <li><b>the named-user door</b> - the candidate users of the tasks <em>in this batch</em>, filtered
     *       down to those actually watching. Bounded by the batch, not a scan of
     *       {@code task_candidate_user}. A user whose groups intersect nothing reaches their count only
     *       here.</li>
     * </ol>
     * Nothing is counted at this stage.
     */
    private Set<String> affectedSubscribers(
        Set<String> taskIds,
        Set<TaskCandidateGroupEntity> candidateGroupRows,
        Set<TaskCandidateUserEntity> candidateUserRows,
        Set<String> groupsNamedInBatch,
        Set<String> usersNamedInBatch
    ) {
        if (anyTaskVisibleToEveryone(taskIds, candidateGroupRows, candidateUserRows)) {
            // A task with no candidates at all matches the specification's "no candidate users or groups"
            // branch, which everyone matches, so there is nothing to narrow by.
            LOGGER.debug("A task in the batch has no candidates; recomputing for every subscriber");
            return subscriberScopeRegistry.allSubscribers();
        }

        Set<String> affectedGroups = new HashSet<>();
        candidateGroupRows.forEach(row -> affectedGroups.add(row.getGroupId()));
        if (!CollectionUtils.isEmpty(groupsNamedInBatch)) {
            affectedGroups.addAll(groupsNamedInBatch);
        }

        Set<String> affectedUsers = new HashSet<>();
        candidateUserRows.forEach(row -> affectedUsers.add(row.getUserId()));
        if (!CollectionUtils.isEmpty(usersNamedInBatch)) {
            affectedUsers.addAll(usersNamedInBatch);
        }

        Set<String> affected = new HashSet<>(subscriberScopeRegistry.subscribersHoldingAnyOf(affectedGroups));
        affectedUsers.stream().filter(subscriberScopeRegistry::isRegistered).forEach(affected::add);
        return affected;
    }

    /**
     * True when the batch touched a task that has neither candidate users nor candidate groups. Both
     * collections must be empty for the specification's catch-all branch to match, so a task carrying only
     * candidate <em>users</em> must not trigger this.
     */
    private boolean anyTaskVisibleToEveryone(
        Set<String> taskIds,
        Set<TaskCandidateGroupEntity> candidateGroupRows,
        Set<TaskCandidateUserEntity> candidateUserRows
    ) {
        Set<String> withCandidates = new HashSet<>();
        candidateGroupRows.forEach(row -> withCandidates.add(row.getTaskId()));
        candidateUserRows.forEach(row -> withCandidates.add(row.getTaskId()));
        return !withCandidates.containsAll(taskIds);
    }

    /**
     * One bucket's counts: the shared half once, the remainder once, then a sum per member.
     * <p>
     * A member missing from the remainder result has no task naming them individually, which is exactly zero
     * - {@code getOrDefault} is the whole special case, and a group-only subscriber needs no other handling.
     */
    private List<TaskCountChange> queuedCountsFor(List<String> bucketGroups, List<String> members) {
        if (bucketGroups.isEmpty()) {
            // No groups means no shareable half: forGroups() would be an unrestricted count, and an empty
            // JPQL IN list is not portable. Count these subscribers directly with the same specification the
            // REST endpoint uses, which is one query each - acceptable because they share nothing anyway.
            return members
                .stream()
                .map(userId ->
                    TaskCountChange.forQueued(
                        userId,
                        bucketGroups,
                        taskRepository.count(
                            TaskSpecification.restricted(PushedTaskCountFilter.QUEUED, userId, bucketGroups)
                        )
                    )
                )
                .toList();
        }

        long shared = taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, bucketGroups));
        Map<String, Long> remainder = remainderFor(bucketGroups, members);

        return members
            .stream()
            .map(userId -> TaskCountChange.forQueued(userId, bucketGroups, shared + remainder.getOrDefault(userId, 0L)))
            .toList();
    }

    private Map<String, Long> remainderFor(List<String> bucketGroups, List<String> members) {
        return taskCandidateUserRepository
            .countTasksNamingUserOutsideGroups(members, PushedTaskCountFilter.QUEUED.status(), bucketGroups)
            .stream()
            .collect(Collectors.toMap(CandidateUserTaskCount::getUserId, CandidateUserTaskCount::getCount));
    }

    private void warnOnFanOut(int subscribers, int buckets) {
        if (subscribers > fanOutWarnThreshold) {
            LOGGER.warn(
                "Recomputing task counts for {} subscriber(s) across {} group set(s) from one event batch, " +
                    "above the {} threshold: every group set costs two queries and every subscriber costs one " +
                    "message. Raise query.count-scopes.fan-out-warn-threshold if this is expected for this " +
                    "deployment.",
                subscribers,
                buckets,
                fanOutWarnThreshold
            );
        }
    }
}
