package org.activiti.cloud.services.api.commands;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoveProcessVariablesCmd implements Command {

    private final String id;
    private String processInstanceId;
    private List<String> variableNames;

    @JsonCreator
    public RemoveProcessVariablesCmd(@JsonProperty("processInstanceId") String processInstanceId,
                                     @JsonProperty("variables") List<String> variableNames) {
        this.id = UUID.randomUUID().toString();
        this.processInstanceId = processInstanceId;
        this.variableNames = variableNames;
    }

    public String getId() {
        return id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }
}