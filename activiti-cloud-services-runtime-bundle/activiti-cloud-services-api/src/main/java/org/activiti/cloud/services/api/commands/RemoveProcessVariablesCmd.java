package org.activiti.cloud.services.api.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RemoveProcessVariablesCmd implements Command {

    private final String id;
    private String processId;
    private List<String> variableNames;

    @JsonCreator
    public RemoveProcessVariablesCmd(@JsonProperty("processId") String processId,
                                     @JsonProperty("variables") List<String> variableNames) {
        this.id = UUID.randomUUID().toString();
        this.processId = processId;
        this.variableNames = variableNames;
    }

    public String getId() {
        return id;
    }

    public String getProcessId() {
        return processId;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }
}