package org.activiti.cloud.services.api.commands.results;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClaimTaskResults implements CommandResults {


    private String id;
    private String commandId;

    public ClaimTaskResults() {
        this.id = UUID.randomUUID().toString();
    }

    @JsonCreator
    public ClaimTaskResults(@JsonProperty("commandId") String commandId) {
        this();
        this.commandId = commandId;
    }

    public String getId() {
        return id;
    }

    public String getCommandId() {
        return commandId;
    }
}
