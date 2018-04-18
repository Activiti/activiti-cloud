package org.activiti.cloud.connectors.starter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectorProperties {

    @Value("${spring.application.name")
    private String serviceName;

    @Value("${activiti.cloud.service.type:}")
    private String serviceType;

    @Value("${activiti.cloud.service.version:}")
    private String serviceVersion;

    @Value("${activiti.cloud.application.name:}")
    private String activitiAppName;

    @Value("${activiti.cloud.application.version:}")
    private String activitiAppVersion;

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

    public String getActivitiAppName() {
        return activitiAppName;
    }

    public String getActivitiAppVersion() {
        return activitiAppVersion;
    }
}
