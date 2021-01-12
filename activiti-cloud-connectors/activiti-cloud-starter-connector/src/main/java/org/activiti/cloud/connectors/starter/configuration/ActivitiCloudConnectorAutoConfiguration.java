/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.connectors.starter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.connectors.starter.channels.AuditChannels;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorChannelResolver;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorChannelResolverImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorDestinationBuilder;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorDestinationBuilderImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandler;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorHandlerImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationErrorSenderImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationRequestErrorChannelListener;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultChannelResolver;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultChannelResolverImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultDestinationBuilder;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultDestinationBuilderImpl;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSender;
import org.activiti.cloud.connectors.starter.channels.IntegrationResultSenderImpl;
import org.activiti.cloud.connectors.starter.channels.ProcessRuntimeChannels;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableBinding({ProcessRuntimeChannels.class, AuditChannels.class})
@PropertySource("classpath:activiti-cloud-connector.properties")
@EnableConfigurationProperties(ConnectorProperties.class)
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
    public IntegrationRequestErrorChannelListener integrationRequestErrorChannelListener(IntegrationErrorHandler integrationErrorHandler) {
        return new IntegrationRequestErrorChannelListener(integrationErrorHandler);
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
    public IntegrationResultSender integrationResultSender(IntegrationResultChannelResolver integrationChannelResolver, AuditChannels auditChannels,
                                                           ConnectorProperties connectorProperties) {
        return new IntegrationResultSenderImpl(integrationChannelResolver, auditChannels, connectorProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorDestinationBuilder integrationErrorDestinationBuilder(ConnectorProperties connectorProperties) {
        return new IntegrationErrorDestinationBuilderImpl(connectorProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorChannelResolver integrationErrorChannelResolver(BinderAwareChannelResolver resolver,
                                                                           IntegrationErrorDestinationBuilder integrationErrorDestinationBuilder) {
        return new IntegrationErrorChannelResolverImpl(resolver, integrationErrorDestinationBuilder);
    }


    @Bean
    @ConditionalOnMissingBean
    public IntegrationErrorSender integrationErrorSender(IntegrationErrorChannelResolver integrationChannelResolver, AuditChannels auditChannels,
                                                         ConnectorProperties connectorProperties) {
        return new IntegrationErrorSenderImpl(integrationChannelResolver, auditChannels, connectorProperties);
    }

}
