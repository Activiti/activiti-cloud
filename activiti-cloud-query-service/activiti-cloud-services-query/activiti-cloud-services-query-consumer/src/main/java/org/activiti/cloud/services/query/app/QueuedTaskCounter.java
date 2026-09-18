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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.subscription.CountChangedMessage;
import org.activiti.cloud.services.query.subscription.ScopeKeys;

/**
 * Pushed "queued for me" badge: unassigned {@link Task.TaskStatus#CREATED} tasks a user may claim, i.e.
 * ones they are a candidate for through one of their groups, personally as a candidate user, or that
 * have no candidates at all (open to everyone). Owning a task does not make it claimable, so ownership
 * is deliberately not counted.
 *
 * <p>Affected users are bucketed by their group set (users sharing a group set share a bucket), so each
 * bucket costs exactly two queries regardless of its size:
 * <pre>
 *   my count = what everyone in my group set can see        (shared, one query per bucket)
 *            + what only I am named on that my groups cannot (personal remainder, one query per bucket)
 * </pre>
 * The two terms are disjoint by construction, so summing them cannot double-count.
 */
public class QueuedTaskCounter implements PushedCounter {

    private static final Task.TaskStatus QUEUED_STATUS = Task.TaskStatus.CREATED;

    /**
     * Synthetic user id that matches no assignee, owner or candidate user. Feeding it to the restricted
     * specification collapses the visibility predicate to "assignee is null and (a candidate group is in
     * the set or the task has no candidates)" — precisely the group-visible count shared by every member
     * of the bucket, while still reusing the specification's no-candidates branch and distinct semantics.
     */
    private static final String GROUP_VISIBILITY_PROBE_USER_ID = "__queued_shared_visibility_probe__";

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
    public List<CountChangedMessage> countFor(Collection<String> affectedUserIds, Instant asOf) {
        Set<String> userIds = affectedUserIds == null ? Set.of() : new LinkedHashSet<>(affectedUserIds);
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Set<String>, List<String>> membersByGroupSet = new LinkedHashMap<>();
        for (String userId : userIds) {
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
        List<CountChangedMessage> messages = new ArrayList<>(userIds.size());
        for (String userId : userIds) {
            messages.add(new CountChangedMessage(ScopeKeys.queued(userId), countByUser.get(userId), asOf));
        }
        return messages;
    }

    private long sharedGroupVisibleCount(Collection<String> groups) {
        return taskRepository.count(
            TaskSpecification.restricted(queuedSearchRequest(), GROUP_VISIBILITY_PROBE_USER_ID, groups)
        );
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
