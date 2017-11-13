package org.activiti.cloud.services.api.commands.results;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReleaseTaskResults implements CommandResults {

    private String id;
    private String commandId;

    public ReleaseTaskResults() {
        this.id = UUID.randomUUID().toString();
    }

    @JsonCreator
    public ReleaseTaskResults(@JsonProperty("commandId") String commandId) {
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
