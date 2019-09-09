package org.activiti.cloud.connectors.starter.configuration;

import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSenderImpl;
import org.activiti.cloud.connectors.starter.channels.ProcessRuntimeChannels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding({ProcessRuntimeChannels.class})
public class ActivitiCloudConnectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectorProperties connectorProperties() {
        return new ConnectorProperties();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultSender integrationResultSender(BinderAwareChannelResolver resolver,
                                                           ConnectorProperties connectorProperties) {
        return new IntegrationResultSenderImpl(resolver, connectorProperties);
                
    }
}
