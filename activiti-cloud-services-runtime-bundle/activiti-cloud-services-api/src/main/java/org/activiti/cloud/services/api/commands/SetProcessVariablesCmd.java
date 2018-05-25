package org.activiti.cloud.services.api.commands;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SetProcessVariablesCmd implements Command {

    private final String id;
    private String processInstanceId;
    private Map<String, ?> variables;

    @JsonCreator
    public SetProcessVariablesCmd(@JsonProperty("processInstanceId") String processInstanceId,
                                  @JsonProperty("variables") Map<String, ?> variables) {
        this.id = UUID.randomUUID().toString();
        this.processInstanceId = processInstanceId;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Map<String, ?> getVariables() {
        return variables;
    }
}