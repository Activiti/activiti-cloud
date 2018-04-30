package org.activiti.cloud.services.query.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ActivitiEntityMetadata {

    protected String serviceName;
    protected String serviceFullName;
    protected String serviceVersion;
    protected String appName;
    protected String appVersion;

    public ActivitiEntityMetadata(){

    }

    public ActivitiEntityMetadata(String serviceName, String serviceFullName, String serviceVersion,
                                  String appName, String appVersion){
        this.serviceName = serviceName;
        this.serviceFullName = serviceFullName;
        this.serviceVersion = serviceVersion;
        this.appName = appName;
        this.appVersion = appVersion;

    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
}
