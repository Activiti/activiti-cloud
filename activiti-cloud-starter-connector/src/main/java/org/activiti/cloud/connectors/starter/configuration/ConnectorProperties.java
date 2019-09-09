package org.activiti.cloud.connectors.starter.configuration;

import org.springframework.beans.factory.annotation.Value;

public class ConnectorProperties {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${activiti.cloud.service.type:}")
    private String serviceType;

    @Value("${activiti.cloud.service.version:}")
    private String serviceVersion;

    @Value("${activiti.cloud.application.name:}")
    private String appName;

    @Value("${activiti.cloud.application.version:}")
    private String appVersion;

    @Value("${activiti.cloud.mq.destination.separator:_}")
    private String mqDestinationSeparator;

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceFullName(){
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
}
