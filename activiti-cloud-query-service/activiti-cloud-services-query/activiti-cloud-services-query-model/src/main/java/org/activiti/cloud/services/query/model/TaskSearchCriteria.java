package org.activiti.cloud.services.query.model;

import java.util.List;

public record TaskSearchCriteria(
    List<ProcessVariableValueFilter> conditions, List<ProcessVariableKey> processVariableFetchKeys
) {}
