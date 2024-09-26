package org.activiti.cloud.services.query.util;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;

public class TaskSearchRequestBuilder {

    private boolean onlyStandalone;
    private boolean onlyRoot;
    private Set<String> name;
    private Set<String> description;
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

    public TaskSearchRequestBuilder onlyStandalone() {
        this.onlyStandalone = true;
        return this;
    }

    public TaskSearchRequestBuilder onlyRoot() {
        this.onlyRoot = true;
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

    public TaskSearchRequestBuilder withTaskVariableFilters(Set<VariableFilter> taskVariableFilters) {
        this.taskVariableFilters = taskVariableFilters;
        return this;
    }

    public TaskSearchRequestBuilder withProcessVariableFilters(Set<VariableFilter> processVariableFilters) {
        this.processVariableFilters = processVariableFilters;
        return this;
    }

    public TaskSearchRequestBuilder withProcessVariableKeys(Set<ProcessVariableKey> processVariableKeys) {
        this.processVariableKeys = processVariableKeys;
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
                processVariableKeys.addAll(keysFromFilters);
            }
        }
        return new TaskSearchRequest(
            onlyStandalone,
            onlyRoot,
            name,
            description,
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
            processVariableKeys
        );
    }
}
