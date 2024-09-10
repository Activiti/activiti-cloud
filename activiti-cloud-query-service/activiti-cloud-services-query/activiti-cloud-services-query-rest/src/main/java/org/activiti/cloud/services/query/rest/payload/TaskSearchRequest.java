/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public record TaskSearchRequest(
    boolean onlyStandalone, boolean onlyRoot, Set<String> name, Set<String> description, Set<Integer> priority, Set<Task.TaskStatus> status, Set<String> completedBy, Set<String> assignee, Date createdFrom, Date createdTo, Date lastModifiedFrom, Date lastModifiedTo, Date lastClaimedFrom, Date lastClaimedTo, Date dueDateFrom, Date dueDateTo, Date completedFrom, Date completedTo, Set<String> candidateUserId, Set<String> candidateGroupId, Set<VariableFilter> taskVariableFilters, Set<VariableFilter> processVariableFilters, Set<ProcessVariableKey> processVariableKeys
) {}
