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
package org.activiti.cloud.services.query.app;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns one flush window's touched identities into who needs which badge recomputed. Named users
 * and candidate-user rows of touched tasks feed assigned/queued/running-processes; candidate-group
 * membership additionally feeds assigned/queued only, matching {@code ProcessInstanceSpecification}'s
 * user-restriction predicate (initiator, assignee, or candidate-user - never candidate-group).
 * Results are filtered to users {@link ConsumerSubscriberRegistry} says are watching.
 */
public class RecomputeAudienceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecomputeAudienceResolver.class);

    private final ConsumerSubscriberRegistry registry;
    private final TaskCandidateUserRepository taskCandidateUserRepository;
    private final TaskCandidateGroupRepository taskCandidateGroupRepository;
    private final TaskRepository taskRepository;

    public RecomputeAudienceResolver(
        ConsumerSubscriberRegistry registry,
        TaskCandidateUserRepository taskCandidateUserRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository,
        TaskRepository taskRepository
    ) {
        this.registry = registry;
        this.taskCandidateUserRepository = taskCandidateUserRepository;
        this.taskCandidateGroupRepository = taskCandidateGroupRepository;
        this.taskRepository = taskRepository;
    }

    public Map<PushedCountType, Set<String>> resolve(ConsumerRecomputeWindow window) {
        if (registry.size() == 0) {
            return Map.of(
                PushedCountType.ASSIGNED,
                Set.of(),
                PushedCountType.QUEUED,
                Set.of(),
                PushedCountType.PROCESSES,
                Set.of()
            );
        }
        Set<TaskCandidateUserEntity> taskCandidateUsers = window.taskIds().isEmpty()
            ? Set.of()
            : taskCandidateUserRepository.findByTaskIdIn(window.taskIds());
        Set<TaskCandidateGroupEntity> taskCandidateGroups = window.taskIds().isEmpty()
            ? Set.of()
            : taskCandidateGroupRepository.findByTaskIdIn(window.taskIds());
        List<TaskEntity> processTasks = window.processInstanceIds().isEmpty()
            ? List.of()
            : taskRepository.findByProcessInstanceIdIn(window.processInstanceIds());
        Set<TaskCandidateUserEntity> processTaskCandidateUsers = window.processInstanceIds().isEmpty()
            ? Set.of()
            : taskCandidateUserRepository.findByTask_ProcessInstanceIdIn(window.processInstanceIds());
        Set<TaskCandidateGroupEntity> processTaskCandidateGroups = window.processInstanceIds().isEmpty()
            ? Set.of()
            : taskCandidateGroupRepository.findByTask_ProcessInstanceIdIn(window.processInstanceIds());

        Set<String> namedAndCandidateUserAudience = resolveNamedAndCandidateUserAudience(window, taskCandidateUsers);
        Set<String> processTaskAudience = resolveProcessTaskAudience(processTasks, processTaskCandidateUsers);

        Set<String> taskDomainAudience = new HashSet<>(namedAndCandidateUserAudience);
        taskDomainAudience.addAll(processTaskAudience);
        taskDomainAudience.addAll(resolveOpenTaskAudience(window.taskIds(), taskCandidateUsers, taskCandidateGroups));
        taskDomainAudience.addAll(
            resolveOpenProcessTaskAudience(processTasks, processTaskCandidateUsers, processTaskCandidateGroups)
        );
        taskDomainAudience.addAll(
            resolveGroupDoorAudience(touchedGroupsForTasks(taskCandidateGroups, window.touchedGroupIds()))
        );
        taskDomainAudience.addAll(resolveGroupDoorAudience(touchedGroupsForProcesses(processTaskCandidateGroups)));

        Set<String> processesAudience = new HashSet<>(namedAndCandidateUserAudience);
        processesAudience.addAll(processTaskAudience);
        addWatched(processesAudience, window.namedInitiatorIds());

        Map<PushedCountType, Set<String>> audience = new EnumMap<>(PushedCountType.class);
        audience.put(PushedCountType.ASSIGNED, Set.copyOf(taskDomainAudience));
        audience.put(PushedCountType.QUEUED, Set.copyOf(taskDomainAudience));
        audience.put(PushedCountType.PROCESSES, Set.copyOf(processesAudience));
        LOGGER.debug(
            "Resolved recompute audience: assigned={}, queued={}, processes={}",
            taskDomainAudience.size(),
            taskDomainAudience.size(),
            processesAudience.size()
        );
        return audience;
    }

    private Set<String> resolveNamedAndCandidateUserAudience(
        ConsumerRecomputeWindow window,
        Set<TaskCandidateUserEntity> taskCandidateUsers
    ) {
        Set<String> audience = new HashSet<>();
        addWatched(audience, window.namedUserIds());
        for (TaskCandidateUserEntity candidate : taskCandidateUsers) {
            addIfWatched(audience, candidate.getUserId());
        }
        return audience;
    }

    /** Current assignee and candidate-users of every task still under a touched process. */
    private Set<String> resolveProcessTaskAudience(
        List<TaskEntity> processTasks,
        Set<TaskCandidateUserEntity> processTaskCandidateUsers
    ) {
        Set<String> audience = new HashSet<>();
        for (TaskEntity task : processTasks) {
            addIfWatched(audience, task.getAssignee());
        }
        for (TaskCandidateUserEntity candidate : processTaskCandidateUsers) {
            addIfWatched(audience, candidate.getUserId());
        }
        return audience;
    }

    /**
     * Every watched user, if a touched task is unassigned and has no candidate user or group at
     * all - {@code TaskSpecification}'s catch-all queued-visibility term, matching everybody.
     */
    private Set<String> resolveOpenTaskAudience(
        Set<String> taskIds,
        Set<TaskCandidateUserEntity> taskCandidateUsers,
        Set<TaskCandidateGroupEntity> taskCandidateGroups
    ) {
        Set<String> candidateFreeTaskIds = candidateFreeTaskIds(taskIds, taskCandidateUsers, taskCandidateGroups);
        if (candidateFreeTaskIds.isEmpty()) {
            return Set.of();
        }
        for (TaskEntity task : taskRepository.findAllById(candidateFreeTaskIds)) {
            if (task.getAssignee() == null) {
                return Set.copyOf(registry.watchedUserIds());
            }
        }
        return Set.of();
    }

    /** Same rule, for tasks reached through a touched process - their state is already loaded, so no extra query. */
    private Set<String> resolveOpenProcessTaskAudience(
        List<TaskEntity> processTasks,
        Set<TaskCandidateUserEntity> processTaskCandidateUsers,
        Set<TaskCandidateGroupEntity> processTaskCandidateGroups
    ) {
        Set<String> tasksWithCandidates = new HashSet<>();
        for (TaskCandidateUserEntity candidate : processTaskCandidateUsers) {
            tasksWithCandidates.add(candidate.getTaskId());
        }
        for (TaskCandidateGroupEntity candidate : processTaskCandidateGroups) {
            tasksWithCandidates.add(candidate.getTaskId());
        }
        for (TaskEntity task : processTasks) {
            if (task.getAssignee() == null && !tasksWithCandidates.contains(task.getId())) {
                return Set.copyOf(registry.watchedUserIds());
            }
        }
        return Set.of();
    }

    private Set<String> candidateFreeTaskIds(
        Set<String> taskIds,
        Set<TaskCandidateUserEntity> taskCandidateUsers,
        Set<TaskCandidateGroupEntity> taskCandidateGroups
    ) {
        Set<String> candidateFreeTaskIds = new HashSet<>(taskIds);
        for (TaskCandidateUserEntity candidate : taskCandidateUsers) {
            candidateFreeTaskIds.remove(candidate.getTaskId());
        }
        for (TaskCandidateGroupEntity candidate : taskCandidateGroups) {
            candidateFreeTaskIds.remove(candidate.getTaskId());
        }
        return candidateFreeTaskIds;
    }

    private Set<String> touchedGroupsForTasks(
        Set<TaskCandidateGroupEntity> taskCandidateGroups,
        Set<String> explicitlyTouchedGroups
    ) {
        Set<String> touchedGroups = new HashSet<>(explicitlyTouchedGroups);
        for (TaskCandidateGroupEntity candidate : taskCandidateGroups) {
            touchedGroups.add(candidate.getGroupId());
        }
        return touchedGroups;
    }

    private Set<String> touchedGroupsForProcesses(Set<TaskCandidateGroupEntity> processTaskCandidateGroups) {
        Set<String> touchedGroups = new HashSet<>();
        for (TaskCandidateGroupEntity candidate : processTaskCandidateGroups) {
            touchedGroups.add(candidate.getGroupId());
        }
        return touchedGroups;
    }

    private Set<String> resolveGroupDoorAudience(Set<String> touchedGroups) {
        Set<String> audience = new HashSet<>();
        if (!touchedGroups.isEmpty()) {
            for (String userId : registry.watchedUserIds()) {
                if (!Collections.disjoint(registry.groupsOf(userId), touchedGroups)) {
                    audience.add(userId);
                }
            }
        }
        return audience;
    }

    private void addWatched(Set<String> audience, Set<String> candidateUserIds) {
        for (String userId : candidateUserIds) {
            addIfWatched(audience, userId);
        }
    }

    private void addIfWatched(Set<String> audience, String userId) {
        if (userId != null && registry.isWatching(userId)) {
            audience.add(userId);
        }
    }
}
