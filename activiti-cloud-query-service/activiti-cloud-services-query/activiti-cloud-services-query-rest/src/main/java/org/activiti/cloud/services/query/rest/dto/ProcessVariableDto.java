package org.activiti.cloud.services.query.rest.dto;

public record ProcessVariableDto(String processDefinitionKey, String name, Object value) {}
