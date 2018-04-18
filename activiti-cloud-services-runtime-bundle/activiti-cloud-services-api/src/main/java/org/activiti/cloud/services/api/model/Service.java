package org.activiti.cloud.services.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Service {
    private String fullName;
    private String name;
    private String type;
    private String version;

    public Service(){

    }

    public Service(String fullName, String name, String type, String version){
        this.fullName = fullName;
        this.name = name;
        this.type = type;
        this.version = version;
    }

    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

}
