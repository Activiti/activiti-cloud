package org.activiti.cloud.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationDeploymentDescriptor {

    private String id;
    private String applicationName;
    private String applicationVersion;
    private List<ServiceDeploymentDescriptor> serviceDeploymentDescriptors;
    private Date deploymentDate;

    public ApplicationDeploymentDescriptor() {
    }

    public ApplicationDeploymentDescriptor(String id,
                                           String applicationName,
                                           String applicationVersion,
                                           List<ServiceDeploymentDescriptor> serviceDeploymentDescriptors) {
        this.id = id;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.serviceDeploymentDescriptors = serviceDeploymentDescriptors;
        this.deploymentDate = new Date();
    }

    public String getId() {
        return id;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public List<ServiceDeploymentDescriptor> getServiceDeploymentDescriptors() {
        return serviceDeploymentDescriptors;
    }

    public Date getDeploymentDate() {
        return deploymentDate;
    }
}
