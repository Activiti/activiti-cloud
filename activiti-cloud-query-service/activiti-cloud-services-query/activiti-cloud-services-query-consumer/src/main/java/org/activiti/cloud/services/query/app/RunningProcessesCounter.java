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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.count.PushedCounter;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.subscription.ScopeKeys;

/** Pushed count of running processes visible to a user. */
public class RunningProcessesCounter implements PushedCounter {

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final TaskCandidateUserRepository taskCandidateUserRepository;

    public RunningProcessesCounter(
        ProcessInstanceRepository processInstanceRepository,
        TaskRepository taskRepository,
        TaskCandidateUserRepository taskCandidateUserRepository
    ) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.taskCandidateUserRepository = taskCandidateUserRepository;
    }

    @Override
    public ScopeKeys.PushedCountType type() {
        return ScopeKeys.PushedCountType.PROCESSES;
    }

    @Override
    public Map<String, Long> compute(Set<String> affectedUserIds) {
        if (affectedUserIds.isEmpty()) {
            return Map.of();
        }
        ProcessInstance.ProcessInstanceStatus running = ProcessInstance.ProcessInstanceStatus.RUNNING;
        Map<String, Set<String>> visibleProcessIds = new HashMap<>();
        processInstanceRepository
            .findRunningByInitiatorIn(affectedUserIds, running)
            .forEach(row -> addVisible(visibleProcessIds, row.getUserId(), row.getProcessInstanceId()));
        taskRepository
            .findRunningProcessesByAssigneeIn(affectedUserIds, running)
            .forEach(row -> addVisible(visibleProcessIds, row.getUserId(), row.getProcessInstanceId()));
        taskCandidateUserRepository
            .findRunningProcessesByCandidateUserIn(affectedUserIds, running)
            .forEach(row -> addVisible(visibleProcessIds, row.getUserId(), row.getProcessInstanceId()));

        return affectedUserIds
            .stream()
            .collect(
                Collectors.toMap(Function.identity(), userId ->
                    (long) visibleProcessIds.getOrDefault(userId, Set.of()).size()
                )
            );
    }

    private void addVisible(Map<String, Set<String>> visibleProcessIds, String userId, String processInstanceId) {
        visibleProcessIds.computeIfAbsent(userId, key -> new HashSet<>()).add(processInstanceId);
    }
}
