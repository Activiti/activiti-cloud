package org.activiti.cloud.services.api.commands.results;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.activiti.cloud.services.api.model.ProcessInstance;

public class StartProcessInstanceResults implements CommandResults {

    private String id;
    private String commandId;

    private ProcessInstance processInstance;

    public StartProcessInstanceResults() {
        this.id = UUID.randomUUID().toString();
    }

    @JsonCreator
    public StartProcessInstanceResults(@JsonProperty("commandId") String commandId,
                                       @JsonProperty("processInstance") ProcessInstance processInstance) {

        this();
        this.commandId = commandId;
        this.processInstance = processInstance;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }
}
