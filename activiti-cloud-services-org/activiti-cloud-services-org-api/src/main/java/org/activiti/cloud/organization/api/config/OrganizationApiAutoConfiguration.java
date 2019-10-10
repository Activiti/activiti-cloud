package org.activiti.cloud.organization.api.config;

import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganizationApiAutoConfiguration {

    @Bean
    public ConnectorModelType connectorModelType() {
        return new ConnectorModelType();
    }

    @Bean
    public ProcessModelType processModelType() {
        return new ProcessModelType();
    }
    
    
}
