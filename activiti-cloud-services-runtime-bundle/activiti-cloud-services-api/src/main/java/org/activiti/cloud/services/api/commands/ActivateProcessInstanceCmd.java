package org.activiti.cloud.services.api.commands;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ActivateProcessInstanceCmd implements Command {

    private final String id;

    private String processInstanceId;

    @JsonCreator
    public ActivateProcessInstanceCmd(@JsonProperty("processInstanceId") String processInstanceId) {
        this.id = UUID.randomUUID().toString();
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
