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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.ScopeKeys;

/**
 * Pushed "assigned to me" badge: tasks where the user is the assignee and the task is
 * {@link Task.TaskStatus#ASSIGNED}. Returns an absolute assigned-task count for every affected user
 * via a single grouped query, materializing zero for those with no assigned task.
 */
public class AssignedTaskCounter implements PushedCounter {

    private final TaskRepository taskRepository;

    public AssignedTaskCounter(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public ScopeKeys.PushedCountType type() {
        return ScopeKeys.PushedCountType.ASSIGNED;
    }

    @Override
    public Map<String, Long> compute(Set<String> affectedUserIds) {
        if (affectedUserIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> assignedCounts = taskRepository
            .countGroupedByAssignee(affectedUserIds, Task.TaskStatus.ASSIGNED)
            .stream()
            .collect(
                Collectors.toMap(TaskRepository.AssigneeCount::getAssignee, TaskRepository.AssigneeCount::getTaskCount)
            );
        return affectedUserIds
            .stream()
            .collect(Collectors.toMap(Function.identity(), userId -> assignedCounts.getOrDefault(userId, 0L)));
    }
}
