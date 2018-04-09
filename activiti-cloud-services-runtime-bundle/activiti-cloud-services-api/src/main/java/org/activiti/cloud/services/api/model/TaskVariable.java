package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskVariable {

    public enum TaskVariableScope {
        LOCAL, GLOBAL
    }

    private String taskId;

    private String name;

    private String type;

    private Object value;

    private String executionId;

    private TaskVariableScope scope;

    public TaskVariable(){

    }

    public TaskVariable(String taskId, String name, String type, Object value, String executionId, TaskVariableScope scope){
        this.taskId=taskId;
        this.name=name;
        this.type=type;
        this.value=value;
        this.executionId=executionId;
        this.scope=scope;
    }

    public String getName() {
        return name;
    }


    public String getType() {
        return type;
    }
    public Object getValue() {
        return value;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskVariableScope getScope() {
        return scope;
    }
}
