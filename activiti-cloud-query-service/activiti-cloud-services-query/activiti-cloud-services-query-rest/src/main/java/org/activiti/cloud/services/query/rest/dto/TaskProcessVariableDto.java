package org.activiti.cloud.services.query.rest.dto;

import java.util.Collection;
import java.util.Map;

public record TaskProcessVariableDto(
    Collection<TaskDto> tasks, Map<String, Object> processVariablesByProcessInstanceId
) {}
