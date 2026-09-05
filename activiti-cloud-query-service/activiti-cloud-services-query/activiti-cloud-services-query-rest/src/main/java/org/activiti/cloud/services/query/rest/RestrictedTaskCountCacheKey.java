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
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;

public record RestrictedTaskCountCacheKey(
    String authenticatedUserId,
    List<String> authenticatedUserGroups,
    TaskSearchRequest taskSearchRequest
) {
    // requestId is a client-supplied correlation id, normalized away so polls differing only by it reuse the cached count.
    public RestrictedTaskCountCacheKey {
        taskSearchRequest = withoutRequestId(taskSearchRequest);
    }

    private static TaskSearchRequest withoutRequestId(TaskSearchRequest r) {
        if (r.requestId() == null) {
            return r;
        }
        return new TaskSearchRequest(
            null,
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
