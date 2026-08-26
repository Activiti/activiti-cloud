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
package org.activiti.cloud.services.query.rest.payload;

import java.util.Date;
import java.util.Objects;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

//prettier-ignore
public record TaskSearchRequest (
    String requestId,
    boolean onlyStandalone,
    boolean onlyRoot,
    Set<String> id,
    Set<String> parentId,
    Set<String> processInstanceId,
    Set<String> name,
    Set<String> description,
    Set<String> processDefinitionName,
    Set<Integer> priority,
    Set<Task.TaskStatus> status,
    Set<String> completedBy,
    Set<String> assignee,
    Date createdFrom,
    Date createdTo,
    Date lastModifiedFrom,
    Date lastModifiedTo,
    Date lastClaimedFrom,
    Date lastClaimedTo,
    Date dueDateFrom,
    Date dueDateTo,
    Date completedFrom,
    Date completedTo,
    Set<String> candidateUserId,
    Set<String> candidateGroupId,
    Set<VariableFilter> taskVariableFilters,
    Set<VariableFilter> processVariableFilters,
    Set<ProcessVariableKey> processVariableKeys,
    CloudRuntimeEntitySort sort
) implements CloudRuntimeEntityFilterRequest {
    // requestId is a client-supplied correlation id and must be excluded from
    // equals/hashCode so it does not defeat the restricted task count cache key.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskSearchRequest that = (TaskSearchRequest) o;
        return (
            onlyStandalone == that.onlyStandalone &&
            onlyRoot == that.onlyRoot &&
            Objects.equals(id, that.id) &&
            Objects.equals(parentId, that.parentId) &&
            Objects.equals(processInstanceId, that.processInstanceId) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(processDefinitionName, that.processDefinitionName) &&
            Objects.equals(priority, that.priority) &&
            Objects.equals(status, that.status) &&
            Objects.equals(completedBy, that.completedBy) &&
            Objects.equals(assignee, that.assignee) &&
            Objects.equals(createdFrom, that.createdFrom) &&
            Objects.equals(createdTo, that.createdTo) &&
            Objects.equals(lastModifiedFrom, that.lastModifiedFrom) &&
            Objects.equals(lastModifiedTo, that.lastModifiedTo) &&
            Objects.equals(lastClaimedFrom, that.lastClaimedFrom) &&
            Objects.equals(lastClaimedTo, that.lastClaimedTo) &&
            Objects.equals(dueDateFrom, that.dueDateFrom) &&
            Objects.equals(dueDateTo, that.dueDateTo) &&
            Objects.equals(completedFrom, that.completedFrom) &&
            Objects.equals(completedTo, that.completedTo) &&
            Objects.equals(candidateUserId, that.candidateUserId) &&
            Objects.equals(candidateGroupId, that.candidateGroupId) &&
            Objects.equals(taskVariableFilters, that.taskVariableFilters) &&
            Objects.equals(processVariableFilters, that.processVariableFilters) &&
            Objects.equals(processVariableKeys, that.processVariableKeys) &&
            Objects.equals(sort, that.sort)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            onlyStandalone,
            onlyRoot,
            id,
            parentId,
            processInstanceId,
            name,
            description,
            processDefinitionName,
            priority,
            status,
            completedBy,
            assignee,
            createdFrom,
            createdTo,
            lastModifiedFrom,
            lastModifiedTo,
            lastClaimedFrom,
            lastClaimedTo,
            dueDateFrom,
            dueDateTo,
            completedFrom,
            completedTo,
            candidateUserId,
            candidateGroupId,
            taskVariableFilters,
            processVariableFilters,
            processVariableKeys,
            sort
        );
    }
}
