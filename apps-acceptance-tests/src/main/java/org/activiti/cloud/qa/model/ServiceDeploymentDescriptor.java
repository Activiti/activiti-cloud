package org.activiti.cloud.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceDeploymentDescriptor {

    private String name;
    private String version;
    private ServiceType serviceType;

    public ServiceDeploymentDescriptor() {
    }

    public ServiceDeploymentDescriptor(String name,
                                       String version,
                                       ServiceType serviceType) {
        this.name = name;
        this.version = version;
        this.serviceType = serviceType;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }
}