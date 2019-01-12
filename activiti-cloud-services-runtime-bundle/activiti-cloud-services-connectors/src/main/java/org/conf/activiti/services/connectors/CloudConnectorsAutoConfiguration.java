/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.conf.activiti.services.connectors;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.ConnectorActionDefinitionFinder;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.runtime.api.connector.VariablesMatchHelper;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.conf.activiti.runtime.api.ConnectorsAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitProducerProperties;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver.NewDestinationBindingCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Configuration
@AutoConfigureBefore(value = ConnectorsAutoConfiguration.class)
@PropertySource("classpath:config/integration-result-stream.properties")
@ComponentScan("org.activiti.core.common.spring.connector")
public class CloudConnectorsAutoConfiguration {

    @Value("${activiti.spring.cloud.stream.connector.integrationRequestSender.routing-key-expression}")
    private String routingKeyExpression;

    /**
     * Configures routing key expression for dynamic cloud connector destinations if rabbit binder exists
     */
    @Bean
    @ConditionalOnClass(RabbitProducerProperties.class)
    @ConditionalOnMissingBean
    public NewDestinationBindingCallback<RabbitProducerProperties> dynamicConnectorDestinationsBindingCallback() {
        return (channelName, channel, producerProperties, extendedProducerProperties) -> {
            Expression expression = new SpelExpressionParser().parseExpression(routingKeyExpression);
            
            extendedProducerProperties.setRoutingKeyExpression(expression);
        };
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IntegrationContextMessageBuilderFactory integrationContextMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new IntegrationContextMessageBuilderFactory(properties);
    }
    
    @Bean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    @ConditionalOnMissingBean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    public MQServiceTaskBehavior mqServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                                       ApplicationEventPublisher eventPublisher,
                                                       ApplicationContext applicationContext,
                                                       IntegrationContextBuilder integrationContextBuilder,
                                                       ConnectorActionDefinitionFinder connectorActionDefinitionFinder,
                                                       VariablesMatchHelper variablesMatchHelper,
                                                       RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new MQServiceTaskBehavior(integrationContextManager,
                                         eventPublisher,
                                         applicationContext,
                                         integrationContextBuilder,
                                         connectorActionDefinitionFinder,
                                         variablesMatchHelper,
                                         runtimeBundleInfoAppender);
    }
}
