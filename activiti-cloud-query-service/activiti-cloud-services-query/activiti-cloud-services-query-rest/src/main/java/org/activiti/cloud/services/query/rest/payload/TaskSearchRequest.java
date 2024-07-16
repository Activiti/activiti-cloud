package org.activiti.cloud.services.query.rest.payload;

import java.util.Set;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.rest.VariableSearch;

public record TaskSearchRequest(
    boolean standAlone, boolean rootTasksOnly, String name, String description, String processDefinitionName, Integer priority, String status, String completedBy, String createdFrom, String createdTo, String dueDateFrom, String dueDateTo, String completedFrom, String completedTo, Set<VariableSearch> taskVariableValueFilters, Set<ProcessVariableValueFilter> processVariableValueFilters, Set<ProcessVariableKey> processVariableKeys
) {}
