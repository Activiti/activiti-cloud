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

import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public record TaskSearchRequest(
    boolean onlyStandalone, boolean onlyRoot, List<String> name, List<String> description, List<String> processDefinitionName, List<Integer> priority, List<String> status, List<String> completedBy, List<String> assignee, String createdFrom, String createdTo, String lastModifiedFrom, String lastModifiedTo, String lastClaimedFrom, String lastClaimedTo, String dueDateFrom, String dueDateTo, String completedFrom, String completedTo, List<String> candidateUserId, List<String> candidateGroupId, Set<VariableFilter> taskVariableFilters, Set<VariableFilter> processVariableFilters, Set<ProcessVariableKey> processVariableKeys
) {}
