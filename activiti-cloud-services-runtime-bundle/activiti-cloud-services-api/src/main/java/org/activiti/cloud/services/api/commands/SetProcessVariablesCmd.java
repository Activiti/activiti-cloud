package org.activiti.cloud.services.api.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

public class SetProcessVariablesCmd implements Command {

    private final String id;
    private String processId;
    private Map<String, ? extends Object> variables;

    @JsonCreator
    public SetProcessVariablesCmd(@JsonProperty("processId") String processId,
                                  @JsonProperty("variables") Map<String, ? extends Object> variables) {
        this.id = UUID.randomUUID().toString();
        this.processId = processId;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public String getProcessId() {
        return processId;
    }

    public Map<String, ? extends Object> getVariables() {
        return variables;
    }
}