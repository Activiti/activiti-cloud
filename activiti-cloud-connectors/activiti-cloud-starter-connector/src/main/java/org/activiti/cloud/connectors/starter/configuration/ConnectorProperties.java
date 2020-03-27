package org.activiti.cloud.connectors.starter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("activiti.cloud.connector")
public class ConnectorProperties {

    private String serviceName;

    private String serviceType;

    private String serviceVersion;

    private String appName;

    private String appVersion;

    private String mqDestinationSeparator;

    private String resultDestinationOverride;

    private String errorDestinationOverride;

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceFullName() {
        return serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getMqDestinationSeparator() {
        return mqDestinationSeparator;
    }

    public String getResultDestinationOverride() {
        return resultDestinationOverride;
    }

    public void setResultDestinationOverride(String resultDestinationOverride) {
        this.resultDestinationOverride = resultDestinationOverride;
    }

    public String getErrorDestinationOverride() {
        return errorDestinationOverride;
    }

    public void setErrorDestinationOverride(String errorDestinationOverride) {
        this.errorDestinationOverride = errorDestinationOverride;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void setMqDestinationSeparator(String mqDestinationSeparator) {
        this.mqDestinationSeparator = mqDestinationSeparator;
    }

}
