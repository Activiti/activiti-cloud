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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.subscription.ScopeKeys;

/**
 * Pushed "queued for me" badge: unassigned {@link Task.TaskStatus#CREATED} tasks a user may claim —
 * through one of their groups, personally as a candidate user, or open to everyone (no candidates).
 * Ownership does not make a task claimable, so it is not counted. Returns an absolute count for every
 * affected user, materializing zero for those with none.
 */
public class QueuedTaskCounter implements PushedCounter {

    private static final Task.TaskStatus QUEUED_STATUS = Task.TaskStatus.CREATED;

    private final TaskRepository taskRepository;
    private final ConsumerSubscriberRegistry subscriberRegistry;

    public QueuedTaskCounter(TaskRepository taskRepository, ConsumerSubscriberRegistry subscriberRegistry) {
        this.taskRepository = taskRepository;
        this.subscriberRegistry = subscriberRegistry;
    }

    @Override
    public ScopeKeys.PushedCountType type() {
        return ScopeKeys.PushedCountType.QUEUED;
    }

    @Override
    public Map<String, Long> compute(Set<String> affectedUserIds) {
        if (affectedUserIds.isEmpty()) {
            return Map.of();
        }
        Map<Set<String>, List<String>> membersByGroupSet = new LinkedHashMap<>();
        for (String userId : affectedUserIds) {
            Set<String> groups = Set.copyOf(subscriberRegistry.groupsOf(userId));
            membersByGroupSet.computeIfAbsent(groups, unused -> new ArrayList<>()).add(userId);
        }
        Map<String, Long> countByUser = new HashMap<>();
        membersByGroupSet.forEach((groups, members) -> {
            long shared = sharedGroupVisibleCount(groups);
            Map<String, Long> remainderByUser = personalRemainder(members, groups);
            for (String userId : members) {
                countByUser.put(userId, shared + remainderByUser.getOrDefault(userId, 0L));
            }
        });
        return countByUser;
    }

    private long sharedGroupVisibleCount(Collection<String> groups) {
        return taskRepository.count(TaskSpecification.groupVisible(queuedSearchRequest(), groups));
    }

    private Map<String, Long> personalRemainder(Collection<String> members, Collection<String> groups) {
        return taskRepository
            .countQueuedPersonalRemainderGroupedByUser(members, QUEUED_STATUS, groups)
            .stream()
            .collect(Collectors.toMap(TaskRepository.UserCount::getUserId, TaskRepository.UserCount::getTaskCount));
    }

    private static TaskSearchRequest queuedSearchRequest() {
        return new TaskSearchRequest(
            null, // requestId
            false, // onlyStandalone
            false, // onlyRoot
            null, // id
            null, // parentId
            null, // processInstanceId
            null, // name
            null, // description
            null, // processDefinitionName
            null, // priority
            Set.of(QUEUED_STATUS), // status
            null, // completedBy
            null, // assignee
            null, // createdFrom
            null, // createdTo
            null, // lastModifiedFrom
            null, // lastModifiedTo
            null, // lastClaimedFrom
            null, // lastClaimedTo
            null, // dueDateFrom
            null, // dueDateTo
            null, // completedFrom
            null, // completedTo
            null, // candidateUserId
            null, // candidateGroupId
            null, // taskVariableFilters
            null, // processVariableFilters
            null, // processVariableKeys
            null // sort
        );
    }
}
