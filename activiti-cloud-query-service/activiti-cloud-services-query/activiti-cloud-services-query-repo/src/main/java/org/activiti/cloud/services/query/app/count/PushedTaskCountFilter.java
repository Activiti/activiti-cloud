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

import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;

/**
 * The one filter whose count is pushed.
 * <p>
 * A pushed count has to be pinned to a single agreed filter. The event consumer has no request to read
 * a filter from, and a count computed with a different filter than the client is displaying is simply a
 * wrong number - so producer and consumer must agree here, statically. Any client wanting a different
 * filter has to keep asking for it over REST.
 * <p>
 * {@link #QUEUED} is the "Queued" badge: unassigned, still open tasks. It matches the group branch of
 * {@code TaskSpecification.forGroups}, which already restricts to unassigned tasks, so the status
 * filter is what excludes tasks that have since been completed or cancelled.
 */
public final class PushedTaskCountFilter {

    /**
     * Unassigned, still-open tasks. Every other field is left unset so the specification applies no
     * further narrowing.
     */
    //prettier-ignore
    public static final TaskSearchRequest QUEUED = new TaskSearchRequest(
        "pushed-queued-count", // requestId
        false,                 // onlyStandalone
        false,                 // onlyRoot
        null,                  // id
        null,                  // parentId
        null,                  // processInstanceId
        null,                  // name
        null,                  // description
        null,                  // processDefinitionName
        null,                  // priority
        Set.of(Task.TaskStatus.CREATED), // status
        null,                  // completedBy
        null,                  // assignee
        null,                  // createdFrom
        null,                  // createdTo
        null,                  // lastModifiedFrom
        null,                  // lastModifiedTo
        null,                  // lastClaimedFrom
        null,                  // lastClaimedTo
        null,                  // dueDateFrom
        null,                  // dueDateTo
        null,                  // completedFrom
        null,                  // completedTo
        null,                  // candidateUserId
        null,                  // candidateGroupId
        null,                  // taskVariableFilters
        null,                  // processVariableFilters
        null,                  // processVariableKeys
        null                   // sort
    );

    private PushedTaskCountFilter() {}
}
