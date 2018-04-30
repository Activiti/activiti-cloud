package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessInstanceVariable {

    private String processInstanceId;

    private String name;

    private String type;

    private Object value;

    private String executionId;

    public ProcessInstanceVariable(){

    }

    public ProcessInstanceVariable(String processInstanceId, String name, String type, Object value, String executionId){
        this.name=name;
        this.type=type;
        this.value=value;
        this.executionId=executionId;
        this.processInstanceId=processInstanceId;
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

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }
}
