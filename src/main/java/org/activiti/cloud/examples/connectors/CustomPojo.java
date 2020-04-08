package org.activiti.cloud.examples.connectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomPojo {

    @JsonProperty("test-json-variable-element1")
    private String testJsonVariableElement1;

    public String getTestJsonVariableElement1() {
        return testJsonVariableElement1;
    }

    public void setTestJsonVariableElement1(String testJsonVariableElement1) {
        this.testJsonVariableElement1 = testJsonVariableElement1;
    }
}
