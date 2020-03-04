package org.activiti.cloud.connectors.starter.configuration;

import org.activiti.cloud.connectors.starter.channels.ErrorChannelServiceActivator;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultChannelResolverImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorChannelResolver;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorChannelResolverImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorDestinationBuilder;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandler;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandlerImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSenderImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultChannelResolver;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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
                                                           ConnectorProperties connectorProperties,
                                                           ObjectMapper objectMapper) {
        return new IntegrationErrorHandlerImpl(integrationErrorSender,
                                               connectorProperties,
                                               objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorChannelServiceActivator errorChannelServiceActivator(IntegrationErrorHandler integrationErrorHandler) {
        return new ErrorChannelServiceActivator(integrationErrorHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultDestinationBuilder integrationResultDestinationBuilder(ConnectorProperties connectorProperties) {
        return new IntegrationResultDestinationBuilderImpl(connectorProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultChannelResolver integrationResultChannelResolver(BinderAwareChannelResolver resolver,
                                                                             IntegrationResultDestinationBuilder integrationResultDestinationBuilder) {
        return new IntegrationResultChannelResolverImpl(resolver, integrationResultDestinationBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationResultSender integrationResultSender(IntegrationResultChannelResolver integrationChannelResolver) {
        return new IntegrationResultSenderImpl(integrationChannelResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorChannelResolver integrationErrorChannelResolver(BinderAwareChannelResolver resolver,
                                                                           IntegrationErrorDestinationBuilder integrationErrorDestinationBuilder) {
        return new IntegrationErrorChannelResolverImpl(resolver, integrationErrorDestinationBuilder);
    }


    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorSender integrationErrorSender(IntegrationErrorChannelResolver integrationChannelResolver) {
        return new IntegrationErrorSenderImpl(integrationChannelResolver);
    }

}
