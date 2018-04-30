package org.activiti.cloud.services.api.commands.results;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class RemoveProcessVariablesResults implements CommandResults {

    private String id;
    private String commandId;


    public RemoveProcessVariablesResults() {
        this.id = UUID.randomUUID().toString();
    }

    @JsonCreator
    public RemoveProcessVariablesResults(@JsonProperty("commandId") String commandId) {
        this();
        this.commandId = commandId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }
}