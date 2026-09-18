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
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.subscription.ScopeKeys.PushedCountType;

/**
 * Phase 2's audience resolution: turns one flush window's touched identities into who actually
 * needs which badge recomputed, per the "two doors" the recompute pipeline is built on.
 *
 * <p>The <b>named-user door</b>: users a captured event named directly, plus a read-back of
 * candidate-user rows limited to this window's touched tasks - never a scan of the whole candidate
 * table. The <b>group door</b>: groups a captured event named directly, plus a read-back of
 * candidate-group rows for the same touched tasks, matched against every watched user whose group
 * set intersects them. Both doors are filtered to users {@link ConsumerSubscriberRegistry} says are
 * actually watching - a group member who isn't watching gets nothing, which is correct: nobody is
 * looking at their badge.
 *
 * <p>Assigned and queued use both doors. Running-processes uses the named-user door, named
 * initiators, and every current assignee/candidate-user of any task in a touched process instance -
 * matching {@code ProcessInstanceSpecification}'s own user-restriction predicate (initiator,
 * assignee, or candidate-user), never the group door: candidate-group membership alone grants no
 * process visibility.
 */
public class RecomputeAudienceResolver {

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
        Set<String> namedAndCandidateUserAudience = resolveNamedAndCandidateUserAudience(window);

        Set<String> taskDomainAudience = new HashSet<>(namedAndCandidateUserAudience);
        taskDomainAudience.addAll(resolveGroupDoorAudience(window));

        Set<String> processesAudience = new HashSet<>(namedAndCandidateUserAudience);
        processesAudience.addAll(resolveProcessDomain(window));

        Map<PushedCountType, Set<String>> audience = new EnumMap<>(PushedCountType.class);
        audience.put(PushedCountType.ASSIGNED, Set.copyOf(taskDomainAudience));
        audience.put(PushedCountType.QUEUED, Set.copyOf(taskDomainAudience));
        audience.put(PushedCountType.PROCESSES, Set.copyOf(processesAudience));
        return audience;
    }

    private Set<String> resolveNamedAndCandidateUserAudience(ConsumerRecomputeWindow window) {
        Set<String> audience = new HashSet<>();
        addWatched(audience, window.namedUserIds());

        if (!window.taskIds().isEmpty()) {
            for (TaskCandidateUserEntity candidate : taskCandidateUserRepository.findByTaskIdIn(window.taskIds())) {
                addIfWatched(audience, candidate.getUserId());
            }
        }
        return audience;
    }

    private Set<String> resolveGroupDoorAudience(ConsumerRecomputeWindow window) {
        Set<String> touchedGroups = new HashSet<>(window.touchedGroupIds());
        if (!window.taskIds().isEmpty()) {
            for (TaskCandidateGroupEntity candidate : taskCandidateGroupRepository.findByTaskIdIn(window.taskIds())) {
                touchedGroups.add(candidate.getGroupId());
            }
        }

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

    private Set<String> resolveProcessDomain(ConsumerRecomputeWindow window) {
        Set<String> audience = new HashSet<>();
        addWatched(audience, window.namedInitiatorIds());

        if (!window.processInstanceIds().isEmpty()) {
            for (TaskEntity task : taskRepository.findByProcessInstanceIdIn(window.processInstanceIds())) {
                addIfWatched(audience, task.getAssignee());
            }
            for (TaskCandidateUserEntity candidate : taskCandidateUserRepository.findByTask_ProcessInstanceIdIn(
                window.processInstanceIds()
            )) {
                addIfWatched(audience, candidate.getUserId());
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
