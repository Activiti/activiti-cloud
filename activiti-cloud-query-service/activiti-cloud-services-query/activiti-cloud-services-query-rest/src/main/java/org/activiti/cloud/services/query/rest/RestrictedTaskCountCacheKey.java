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
package org.activiti.cloud.services.query.rest;

import java.util.List;
import java.util.Objects;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;

public record RestrictedTaskCountCacheKey(
    String authenticatedUserId,
    List<String> authenticatedUserGroups,
    TaskSearchRequest taskSearchRequest
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestrictedTaskCountCacheKey that = (RestrictedTaskCountCacheKey) o;
        return (
            Objects.equals(authenticatedUserId, that.authenticatedUserId) &&
            Objects.equals(authenticatedUserGroups, that.authenticatedUserGroups) &&
            taskSearchRequestEqualsIgnoringRequestId(taskSearchRequest, that.taskSearchRequest)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticatedUserId, authenticatedUserGroups, taskSearchRequestHashCodeIgnoringRequestId(taskSearchRequest));
    }

    private static boolean taskSearchRequestEqualsIgnoringRequestId(TaskSearchRequest a, TaskSearchRequest b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return (
            a.onlyStandalone() == b.onlyStandalone() &&
            a.onlyRoot() == b.onlyRoot() &&
            Objects.equals(a.id(), b.id()) &&
            Objects.equals(a.parentId(), b.parentId()) &&
            Objects.equals(a.processInstanceId(), b.processInstanceId()) &&
            Objects.equals(a.name(), b.name()) &&
            Objects.equals(a.description(), b.description()) &&
            Objects.equals(a.processDefinitionName(), b.processDefinitionName()) &&
            Objects.equals(a.priority(), b.priority()) &&
            Objects.equals(a.status(), b.status()) &&
            Objects.equals(a.completedBy(), b.completedBy()) &&
            Objects.equals(a.assignee(), b.assignee()) &&
            Objects.equals(a.createdFrom(), b.createdFrom()) &&
            Objects.equals(a.createdTo(), b.createdTo()) &&
            Objects.equals(a.lastModifiedFrom(), b.lastModifiedFrom()) &&
            Objects.equals(a.lastModifiedTo(), b.lastModifiedTo()) &&
            Objects.equals(a.lastClaimedFrom(), b.lastClaimedFrom()) &&
            Objects.equals(a.lastClaimedTo(), b.lastClaimedTo()) &&
            Objects.equals(a.dueDateFrom(), b.dueDateFrom()) &&
            Objects.equals(a.dueDateTo(), b.dueDateTo()) &&
            Objects.equals(a.completedFrom(), b.completedFrom()) &&
            Objects.equals(a.completedTo(), b.completedTo()) &&
            Objects.equals(a.candidateUserId(), b.candidateUserId()) &&
            Objects.equals(a.candidateGroupId(), b.candidateGroupId()) &&
            Objects.equals(a.taskVariableFilters(), b.taskVariableFilters()) &&
            Objects.equals(a.processVariableFilters(), b.processVariableFilters()) &&
            Objects.equals(a.processVariableKeys(), b.processVariableKeys()) &&
            Objects.equals(a.sort(), b.sort())
        );
    }

    private static int taskSearchRequestHashCodeIgnoringRequestId(TaskSearchRequest r) {
        if (r == null) {
            return 0;
        }
        return Objects.hash(
            r.onlyStandalone(),
            r.onlyRoot(),
            r.id(),
            r.parentId(),
            r.processInstanceId(),
            r.name(),
            r.description(),
            r.processDefinitionName(),
            r.priority(),
            r.status(),
            r.completedBy(),
            r.assignee(),
            r.createdFrom(),
            r.createdTo(),
            r.lastModifiedFrom(),
            r.lastModifiedTo(),
            r.lastClaimedFrom(),
            r.lastClaimedTo(),
            r.dueDateFrom(),
            r.dueDateTo(),
            r.completedFrom(),
            r.completedTo(),
            r.candidateUserId(),
            r.candidateGroupId(),
            r.taskVariableFilters(),
            r.processVariableFilters(),
            r.processVariableKeys(),
            r.sort()
        );
    }
}
