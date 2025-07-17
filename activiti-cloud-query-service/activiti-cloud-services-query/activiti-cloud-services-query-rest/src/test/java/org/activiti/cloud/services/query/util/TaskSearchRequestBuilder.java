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
package org.activiti.cloud.services.query.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class TaskSearchRequestBuilder {

    private boolean onlyStandalone;
    private boolean onlyRoot;
    private Set<String> id;
    private Set<String> parentId;
    private Set<String> processInstanceId;
    private Set<String> name;
    private Set<String> description;
    private Set<String> processDefinitionName;
    private Set<Integer> priority;
    private Set<Task.TaskStatus> status;
    private Set<String> completedBy;
    private Set<String> assignee;
    private Date createdFrom;
    private Date createdTo;
    private Date lastModifiedFrom;
    private Date lastModifiedTo;
    private Date lastClaimedFrom;
    private Date lastClaimedTo;
    private Date dueDateFrom;
    private Date dueDateTo;
    private Date completedFrom;
    private Date completedTo;
    private Set<String> candidateUserId;
    private Set<String> candidateGroupId;
    private Set<VariableFilter> taskVariableFilters;
    private Set<VariableFilter> processVariableFilters;
    private Set<ProcessVariableKey> processVariableKeys;
    private CloudRuntimeEntitySort sort;

    public TaskSearchRequestBuilder onlyStandalone() {
        this.onlyStandalone = true;
        return this;
    }

    public TaskSearchRequestBuilder onlyRoot() {
        this.onlyRoot = true;
        return this;
    }

    public TaskSearchRequestBuilder withId(String... ids) {
        this.id = Set.of(ids);
        return this;
    }

    public TaskSearchRequestBuilder withParentId(String... parentIds) {
        this.parentId = Set.of(parentIds);
        return this;
    }

    public TaskSearchRequestBuilder withProcessInstanceId(String... processInstanceIds) {
        this.processInstanceId = Set.of(processInstanceIds);
        return this;
    }

    public TaskSearchRequestBuilder withName(String... names) {
        this.name = Set.of(names);
        return this;
    }

    public TaskSearchRequestBuilder withDescription(String... descriptions) {
        this.description = Set.of(descriptions);
        return this;
    }

    public TaskSearchRequestBuilder withProcessDefinitionName(String... processDefinitionNames) {
        this.processDefinitionName = Set.of(processDefinitionNames);
        return this;
    }

    public TaskSearchRequestBuilder withPriority(Integer... priorities) {
        this.priority = Set.of(priorities);
        return this;
    }

    public TaskSearchRequestBuilder withStatus(Task.TaskStatus... statuses) {
        this.status = Set.of(statuses);
        return this;
    }

    public TaskSearchRequestBuilder withCompletedBy(String... completedBy) {
        this.completedBy = Set.of(completedBy);
        return this;
    }

    public TaskSearchRequestBuilder withAssignees(String... assignees) {
        this.assignee = Set.of(assignees);
        return this;
    }

    public TaskSearchRequestBuilder withCreatedFrom(Date createdFrom) {
        this.createdFrom = createdFrom;
        return this;
    }

    public TaskSearchRequestBuilder withCreatedTo(Date createdTo) {
        this.createdTo = createdTo;
        return this;
    }

    public TaskSearchRequestBuilder withLastModifiedFrom(Date lastModifiedFrom) {
        this.lastModifiedFrom = lastModifiedFrom;
        return this;
    }

    public TaskSearchRequestBuilder withLastModifiedTo(Date lastModifiedTo) {
        this.lastModifiedTo = lastModifiedTo;
        return this;
    }

    public TaskSearchRequestBuilder withLastClaimedFrom(Date lastClaimedFrom) {
        this.lastClaimedFrom = lastClaimedFrom;
        return this;
    }

    public TaskSearchRequestBuilder withLastClaimedTo(Date lastClaimedTo) {
        this.lastClaimedTo = lastClaimedTo;
        return this;
    }

    public TaskSearchRequestBuilder withDueDateFrom(Date dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
        return this;
    }

    public TaskSearchRequestBuilder withDueDateTo(Date dueDateTo) {
        this.dueDateTo = dueDateTo;
        return this;
    }

    public TaskSearchRequestBuilder withCompletedFrom(Date completedFrom) {
        this.completedFrom = completedFrom;
        return this;
    }

    public TaskSearchRequestBuilder withCompletedTo(Date completedTo) {
        this.completedTo = completedTo;
        return this;
    }

    public TaskSearchRequestBuilder withCandidateUserId(String... candidateUserIds) {
        this.candidateUserId = Set.of(candidateUserIds);
        return this;
    }

    public TaskSearchRequestBuilder withCandidateGroupId(String... candidateGroupIds) {
        this.candidateGroupId = Set.of(candidateGroupIds);
        return this;
    }

    public TaskSearchRequestBuilder withTaskVariableFilters(VariableFilter... taskVariableFilters) {
        this.taskVariableFilters = Set.of(taskVariableFilters);
        return this;
    }

    public TaskSearchRequestBuilder withProcessVariableFilters(VariableFilter... processVariableFilters) {
        this.processVariableFilters = Set.of(processVariableFilters);
        return this;
    }

    public TaskSearchRequestBuilder withProcessVariableKeys(ProcessVariableKey... processVariableKeys) {
        this.processVariableKeys = Set.of(processVariableKeys);
        return this;
    }

    public TaskSearchRequestBuilder withSort(CloudRuntimeEntitySort sort) {
        this.sort = sort;
        return this;
    }

    public TaskSearchRequestBuilder invertSort() {
        if (sort != null) {
            sort =
                new CloudRuntimeEntitySort(
                    sort.field(),
                    sort.direction().isAscending() ? "desc" : "asc",
                    sort.isProcessVariable(),
                    sort.processDefinitionKey(),
                    sort.type()
                );
        }
        return this;
    }

    public TaskSearchRequest build() {
        if (processVariableFilters != null) {
            Set<ProcessVariableKey> keysFromFilters = processVariableFilters
                .stream()
                .map(variableFilter ->
                    new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name())
                )
                .collect(Collectors.toSet());
            if (processVariableKeys == null) {
                processVariableKeys = keysFromFilters;
            } else {
                processVariableKeys = new HashSet<>(processVariableKeys);
                processVariableKeys.addAll(keysFromFilters);
            }
        }
        return new TaskSearchRequest(
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

    public String buildJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            return objectMapper.writeValueAsString(build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
