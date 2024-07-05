package org.activiti.cloud.services.query.model;

public class ProcessVariableKey {

    private final String processDefinitionKey;
    private final String variableName;

    public ProcessVariableKey(String processDefinitionKey, String variableName) {
        this.processDefinitionKey = processDefinitionKey;
        this.variableName = variableName;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getVariableName() {
        return variableName;
    }
}
