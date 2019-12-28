package org.activiti.cloud.modeling.api.config;

import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelingApiAutoConfiguration {

    @Bean
    public ConnectorModelType connectorModelType() {
        return new ConnectorModelType();
    }

    @Bean
    public ProcessModelType processModelType() {
        return new ProcessModelType();
    }


}
