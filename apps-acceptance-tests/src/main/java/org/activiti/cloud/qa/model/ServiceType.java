package org.activiti.cloud.qa.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceType {
    AUDIT("audit"),
    QUERY("query"),
    CONNECTOR("connector"),
    RUNTIME_BUNDLE("runtime-bundle");

    private final String value;

    ServiceType(final String type) {
        value = type;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    public boolean equals(ServiceType serviceType){
        return serviceType.toString().equals(value);
    }
}
