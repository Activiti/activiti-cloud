package org.activiti.cloud.connectors.starter.configuration;

import org.activiti.cloud.connectors.starter.channels.ErrorChannelStreamListener;
import org.activiti.cloud.connectors.starter.channels.IntegrationChannelResolver;
import org.activiti.cloud.connectors.starter.channels.IntegrationChannelResolverImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandler;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandlerImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSenderImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultDestinationBuilder;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultDestinationBuilderImpl;
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
    public IntegrationErrorHandler integrationErrorHandler(IntegrationErrorSender integrationErrorSender,
                                                           ConnectorProperties connectorProperties) {
        return new IntegrationErrorHandlerImpl(integrationErrorSender,
                                               connectorProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ErrorChannelStreamListener errorChannelStreamListener(IntegrationErrorHandler integrationErrorHandler) {
        return new ErrorChannelStreamListener(integrationErrorHandler);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultDestinationBuilder integrationResultDestinationBuilder(ConnectorProperties connectorProperties) {
        return new IntegrationResultDestinationBuilderImpl(connectorProperties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IntegrationChannelResolver integrationChannelResolver(BinderAwareChannelResolver resolver,
                                                                 IntegrationResultDestinationBuilder integrationResultDestinationBuilder) {
        return new IntegrationChannelResolverImpl(resolver, integrationResultDestinationBuilder);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultSender integrationResultSender(IntegrationChannelResolver integrationChannelResolver) {
        return new IntegrationResultSenderImpl(integrationChannelResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorSender integrationErrorSender(IntegrationChannelResolver integrationChannelResolver) {
        return new IntegrationErrorSenderImpl(integrationChannelResolver);
    }
    
}
